package controllers.auth

import api.ApiError
import api.ApiError._
import play.api.libs.json._
import common.log.PackageLogging
import play.api.libs.json.{JsString, JsObject, Json}
import play.api.mvc._
import securesocial.core.SecureSocial
import org.bson.types.ObjectId
import org.corespring.platform.core.models.{User, Organization}

/**
 * A class that adds an AuthorizationContext to the Request object
 * @param ctx - the AuthorizationContext
 * @param r - the Request
 * @tparam A - the type determining the type of the body parser (eg: AnyContent)
 */
case class ApiRequest[A](ctx: AuthorizationContext, r: Request[A], token : String) extends WrappedRequest(r)

/**
 * A base trait for all objects implementing API calls.  Intercepts the request and extracts the credentials of the caller
 * either from an OAuth token or the Play session.  If the credentials are valid creates an AuthorizationContext that is passed
 * to the call wrapped by ApiAction
 *
 * @see AuthorizationContext
 * @see Permission
 * @see PermissionSet
 */
trait BaseApi extends Controller with SecureSocial with PackageLogging{

  private val AuthorizationHeader = "Authorization"
  private val Bearer = "Bearer"
  private val Space = " "



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

  def SSLApiAction[A](p:BodyParser[A])(f:ApiRequest[A] => Result):Action[A] = ApiAction(p){
    request =>
      request.headers.get("x-forwarded-proto") match {
        case Some("https") => f(request)
        case _ => BadRequest(JsObject(Seq("error" -> JsString("must access api calls through https"))))
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
        Logger.debug("request route: "+request.method+" "+request.uri)
        SecureSocial.currentUser(request).find(_ => request.headers.get("CoreSpring-IgnoreSession").isEmpty).map { u =>
          invokeAsUser(u.id.id, u.id.providerId, request)(f)
        }.getOrElse {
          tokenFromRequest(request).fold(error => BadRequest(Json.toJson(error)), token =>
            OAuthProvider.getAuthorizationContext(token).fold(
              error => {
                Logger.debug("Error getting authorization context")
                Forbidden(Json.toJson(error)).as(JSON)
              },
              ctx => {
                val result: PlainResult = f(ApiRequest(ctx, request, token)).asInstanceOf[PlainResult]
                Logger.debug("returning result")
                result
              }
            )
          )
        }
    }
  }

  private def ApiActionPermissions[A](p: BodyParser[A])(access:Permission)(f: ApiRequest[A] => Result)= {
    Action(p) {
      request =>
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
      def orgId : Option[ObjectId] = User.getUser(username, provider).map(_.org.orgId)

      val maybeOrg : Option[Organization] = orgId.map(Organization.findOneById).getOrElse(None)
      maybeOrg.map{ org =>
        val ctx = new AuthorizationContext(org.id, Option(username), true, Some(org))
        f( ApiRequest(ctx, request, ""))
      }.getOrElse( Forbidden( Json.toJson(MissingCredentials) ).as(JSON) )
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

  def parsed[A](maybeJson:Option[JsValue], fn: (A=>Result), noItemResult: Result = BadRequest("Bad Json"))(implicit format : Format[A]) : Result = maybeJson match {
    case Some(json) => {
      play.api.libs.json.Json.fromJson[A](json) match {
        case JsSuccess(item, _) => fn(item)
        case _ => noItemResult
      }
    }
    case _ => noItemResult
  }
}
