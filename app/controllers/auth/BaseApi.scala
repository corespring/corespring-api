package controllers.auth

import play.api.mvc._
import play.api.{Logger, Play}
import api.ApiError
import api.ApiError._
import play.api.libs.json.Json
import org.bson.types.ObjectId
import models.User
import securesocial.core.{SecuredRequest, SecureSocial}


/**
 * A base trait for all objects implementing API calls.  Intercepts the request and extracts the credentials of the caller
 * either from an OAuth token or the Play session.  If the credentials are valid creates an AuthorizationContext that is passed
 * to the call wrapped by ApiAction
 *
 * @see AuthorizationContext
 * @see Permission
 * @see PermissionSet
 */
trait BaseApi extends Controller with SecureSocial{

  val AuthorizationHeader = "Authorization"
  val Bearer = "Bearer"
  val Space = " "

  /**
   * A class that adds an AuthorizationContext to the Request object
   * @param ctx - the AuthorizationContext
   * @param r - the Request
   * @tparam A - the type determining the type of the body parser (eg: AnyContent)
   */
  case class ApiRequest[A](ctx: AuthorizationContext, r: Request[A], token : String) extends WrappedRequest(r)


  /**
   * Returns the access token either from the Play session (with key access_token) or from the Authorization header
   * in the form of "Bearer some_token"
   *
   * @param request
   * @tparam A
   * @return
   */
  def tokenFromRequest[A](request: Request[A]): Either[ApiError, String] = {
    request.queryString.get(OAuthConstants.AccessToken).map(seq => Right(seq.head)).getOrElse {
    request.session.get(OAuthConstants.AccessToken).map(Right(_)).getOrElse {
        request.headers.get(AuthorizationHeader) match {
          case Some(value) =>
            value.split(Space) match {
              case Array(Bearer, token) => Right(token)
              case _ => Left(InvalidTokenType)
            }
          case _ => Left(MissingCredentials)
        }
      }
    }
  }

  /**
   * Creates an authentication context using the information from the header 'sudo'.
   * This can be used in development to bypass the regular authentication flow and test the APIs.
   *
   * for example to invoke as user John in company X you would use the username and the company id as:
   *
   * curl -H  "sudo:john.doe@3e4dda7c-8746-4483-8b58-a5a745a6991f" http://.....
   *
   *
   * @param request
   * @tparam A
   * @return
   */
  def fakeContext[A](request: Request[A]): Either[IllegalArgumentException, Option[AuthorizationContext]] = {
    if (Play.isProd(Play.current)) {
      // we don't want this in production mode
      Right(None)
    } else {
      try {
        Right(request.headers.get("sudo").map {
          _.split("@") match {
            //            case Array(user, "master") =>
            //              Logger.info("creating fake context with org master for user = " + user)
            //              Option(new AuthorizationContext(controllers.GlobalVars.masterOrgId, Option(user)))
            case Array(user, org) =>
              Logger.info("creating fake context with org " + org + ", user = " + user)
              Option(new AuthorizationContext(new ObjectId(org), Option(user)))
            //              case Array("master") =>
            //                Logger.info("creating fake context for master org ")
            //                Option(new AuthorizationContext(controllers.GlobalVars.masterOrgId))
            case Array(org) =>
              Logger.info("creating fake context with org " + org)
              Option(new AuthorizationContext(new ObjectId(org)))
          }
        }.getOrElse(None))
      } catch {
        case ex: IllegalArgumentException => Left(ex)
      }
    }
  }

  def SSLApiAction[A](p:BodyParser[A])(f:ApiRequest[A] => Result):Action[A] = ApiAction(p){
    request =>
      request.headers.get("x-forwarded-proto") match {
        case Some("https") => f(request)
        case _ => Redirect("https://" + request.headers.get("host").get + request.uri)
      }
  }

  def SSLSecuredAction()(f:SecuredRequest[AnyContent] => Result):Action[AnyContent] = SecuredAction(){
    request =>
      request.headers.get("x-forwarded-proto") match {
        case Some("https") => f(request)
        case _ => Redirect("https://" + request.headers.get("host").get + request.uri)
      }
  }
  def SSLAction[A](p:BodyParser[A])(f:Request[A] => Result):Action[A] = Action(p){
    request =>
      request.headers.get("x-forwarded-proto") match {
        case Some("https") => f(request)
        case _ => Redirect("https://" + request.headers.get("host").get + request.uri)
      }
  }

  /**
   * A helper method to create an action for the API calls
   *
   * @param p - the body parser
   * @param f - the method that gets executed if the credentials are ok
   * @tparam A - the type of the body parser (eg: AnyContent)
   * @return a Result or BadRequest if the credentials are invalid
   */
  def ApiAction[A](p: BodyParser[A])(f: ApiRequest[A] => Result) = {
    Action(p) {
      request =>
        fakeContext(request).fold(
          error => BadRequest(Json.toJson(new ApiError(1, "The id specified is not a valid UUID"))),
          optionalCtx => optionalCtx.map(ctx => f(ApiRequest(ctx, request, "fake_token"))).getOrElse {
            SecureSocial.currentUser(request).find(_ => request.headers.get("CoreSpring-IgnoreSession").isEmpty).map { u =>
              invokeAsUser(u.id.id, u.id.providerId, request)(f)
            }.getOrElse {
              tokenFromRequest(request).fold(error => BadRequest(Json.toJson(error)), token =>
                OAuthProvider.getAuthorizationContext(token).fold(
                  error => Forbidden(Json.toJson(error)).as(JSON),
                  ctx => { val result: PlainResult = f(ApiRequest(ctx, request, token)).asInstanceOf[PlainResult]
                    result
                  }
                )
              )
            }
          }
        )
    }
  }

  private def ApiActionPermissions[A](p: BodyParser[A])(access:Permission)(f: ApiRequest[A] => Result)= {
    Action(p) {
      request =>
        fakeContext(request).fold(
          error => BadRequest(Json.toJson(new ApiError(1, "The id specified is not a valid UUID"))),
          optionalCtx => optionalCtx.map(ctx => {
            if(ctx.permission.has(access)) f(ApiRequest(ctx, request, "fake_token"))
            else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization(Some("your registered organization does not have acces to this request"))))
          }).getOrElse {
            SecureSocial.currentUser(request).find(_ => request.headers.get("CoreSpring-IgnoreSession").isEmpty).map( u => {
              invokeAsUser(u.id.id, u.id.providerId, request){request =>
                if(request.ctx.permission.has(access)) f(request)
                else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization(Some("your registered organization does not have acces to this request"))))
              }
            }).getOrElse( tokenFromRequest(request).fold(error => BadRequest(Json.toJson(error)), token =>
                OAuthProvider.getAuthorizationContext(token).fold(
                  error => Forbidden(Json.toJson(error)).as(JSON),
                  ctx => {
                    ctx.permission.has(access)
                    val result: PlainResult = if(ctx.permission.has(access))f(ApiRequest(ctx, request, token)).asInstanceOf[PlainResult]
                                              else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization(Some("your registered organization does not have acces to this request"))))
                    result
                  }
                )
            ))
          }
        )
    }
  }
  /**
   * A helper method to create an action for the API calls
   *
   * @param p - the body parser
   * @param f - the method that gets executed if the credentials are ok
   * @tparam A - the type of the body parser (eg: AnyContent)
   * @return a Result or BadRequest if the credentials are invalid
   */
  def ApiActionRead[A](p: BodyParser[A])(f: ApiRequest[A] => Result) = ApiActionPermissions[A](p)(Permission.Read)(f)
  def ApiActionWrite[A](p: BodyParser[A])(f: ApiRequest[A] => Result) = ApiActionPermissions[A](p)(Permission.Write)(f)

  /**
   * Invokes the action by passing an authorization context created from the Play's session informatino
   *
   * @param username
   * @param request
   * @param f
   * @tparam A
   * @return
   */
    def invokeAsUser[A](username: String, provider:String, request: Request[A])(f: ApiRequest[A]=>Result) = {
      User.getUser(username, provider).map { user =>
        Logger.debug("Using user in Play's session = " + username)
        //TODO: check orgId is right
        val ctx = new AuthorizationContext(user.orgs.head.orgId, Option(username),true)
        f( ApiRequest(ctx, request, ""))
      }.getOrElse(
        Forbidden( Json.toJson(MissingCredentials) ).as(JSON)
      )
    }

  /**
   * A helper method to create an action for API calls
   *
   * @param f - the method that gets executed if the credentials are ok
   * @return a Result or BadRequest if the credentials are invalid
   */
  def ApiAction(f: ApiRequest[AnyContent] => Result): Action[AnyContent] = {
    ApiAction(parse.anyContent)(f)
  }
  def SSLApiAction(f: ApiRequest[AnyContent] => Result): Action[AnyContent] = {
    SSLApiAction(parse.anyContent)(f)
  }
  def ApiActionRead(f: ApiRequest[AnyContent] => Result) = ApiActionPermissions(parse.anyContent)(Permission.Read)(f)
  def ApiActionWrite(f: ApiRequest[AnyContent] => Result) = ApiActionPermissions(parse.anyContent)(Permission.Write)(f)
  def SSLAction(f: Request[AnyContent] => Result): Action[AnyContent] = {
    SSLAction(parse.anyContent)(f)
  }
  /**
   * An action that makes sure the is a user in the authorization context.
   *
   * @param p a Body parser
   * @param block the code that gets executed if the user is present
   * @tparam A The parser type
   * @return Returns the result of the block function or BadRequest if there is no user available in the context
   */
  def ApiActionWithUser[A](p: BodyParser[A])(block: (String, ApiRequest[A]) => Result): Action[A] = ApiAction(p) {
    request =>
      request.ctx.user.map(block(_, request)).getOrElse(BadRequest(Json.toJson(UserIsRequired)))
  }

  /**
   * An action that makes sure the is a user in the authorization context.
   *
   * @param block the code that gets executed if the user is present
   * @return Returns the result of the block function or BadRequest if there is no user available in the context
   */
  def ApiActionWithUser(block: (String, ApiRequest[AnyContent]) => Result): Action[AnyContent] = {
    ApiActionWithUser(parse.anyContent)(block)
  }

  protected def jsonExpected = BadRequest(Json.toJson(ApiError.JsonExpected))
}
