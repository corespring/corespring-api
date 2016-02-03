package org.corespring.platform.core.controllers.auth

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.legacy.ServiceLookup
import org.corespring.models.Organization
import org.corespring.models.auth.{ Permission }
import org.corespring.services.ContentCollectionService
import org.corespring.web.api.v1.errors.ApiError
import play.api.Logger
import play.api.libs.json.{ JsObject, JsString, Json, _ }
import play.api.mvc._
import securesocial.core.SecureSocial

import scala.concurrent.Future
import scalaz.{Validation, Failure, Success}

/**
 * A class that adds an AuthorizationContext to the Request object
 * @param ctx - the AuthorizationContext
 * @param r - the Request
 * @tparam A - the type determining the type of the body parser (eg: AnyContent)
 */
case class ApiRequest[A](ctx: AuthorizationContext, r: Request[A], token: String) extends WrappedRequest(r)

/**
 * A base trait for all objects implementing API calls.  Intercepts the request and extracts the credentials of the caller
 * either from an OAuth token or the Play session.  If the credentials are valid creates an AuthorizationContext that is passed
 * to the call wrapped by ApiAction
 *
 * @see AuthorizationContext
 * @see Permission
 * @see PermissionSet
 */
trait BaseApi
  extends Controller
  with SecureSocial
  with TokenReader {

  def oAuthProvider: OAuthProvider

  protected lazy val logger = Logger(classOf[BaseApi])

  val contentCollection: ContentCollectionService = ServiceLookup.contentCollectionService

  lazy val withoutArchive = MongoDBObject("collectionId" -> MongoDBObject("$ne" -> contentCollection.archiveCollectionId.toString))

  def tokenFromRequest[A](request: Request[A]): Either[ApiError, String] = {
    getToken[ApiError](request, ApiError.InvalidToken, ApiError.MissingCredentials)
  }

  def SSLApiAction[A](p: BodyParser[A])(f: ApiRequest[A] => Result): Action[A] = ApiAction(p) {
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

        val IgnoreSession = "CoreSpring-IgnoreSession"

        logger.trace(s"[ApiAction] request route: ${request.method}, ${request.uri}")
        logger.trace(s"[ApiAction] ignore session: ${request.headers.get(IgnoreSession)}")

        def resultFromToken = {

          def onError(apiError: ApiError) = BadRequest(Json.toJson(apiError))

          def onToken(token: String) = oAuthProvider.getAuthorizationContext(token).fold(
            error => {
              logger.debug("Error getting authorization context")
              Forbidden(Json.toJson(error)).as(JSON)
            },
            ctx => {
              val result: SimpleResult = f(ApiRequest(ctx, request, token)).asInstanceOf[SimpleResult]
              logger.trace("returning result")
              result
            })
          tokenFromRequest(request).fold(onError, onToken)
        }

        def userResult = for {
          currentUser <- SecureSocial.currentUser(request)
          if (request.headers.get(IgnoreSession).isEmpty)
        } yield {
          logger.trace(s"currentUser: $currentUser")
          invokeAsUser(currentUser.identityId.userId, currentUser.identityId.providerId, request)(f)
        }

        userResult.getOrElse(resultFromToken)
    }
  }

  def AsyncApiAction[A](p: BodyParser[A])(f: ApiRequest[A] => Future[SimpleResult]) = {
    Action.async(p) {
      request =>

        val IgnoreSession = "CoreSpring-IgnoreSession"

        logger.trace(s"[AsyncApiAction] request route: ${request.method}, ${request.uri}")
        logger.trace(s"[AsyncApiAction] ignore session: ${request.headers.get(IgnoreSession)}")

        def resultFromToken: Future[SimpleResult] = {

          def onError(apiError: ApiError) = Future.successful(BadRequest(Json.toJson(apiError)))

          def onToken(token: String): Future[SimpleResult] = oAuthProvider.getAuthorizationContext(token).fold(
            error => {
              logger.debug("Error getting authorization context")
              Future.successful(Forbidden(Json.toJson(error)).as(JSON))
            },
            ctx => {
              val result = f(ApiRequest(ctx, request, token))
              logger.trace("returning result")
              result
            })
          tokenFromRequest(request).fold(onError, onToken)
        }

        def userResult: Option[Future[SimpleResult]] = for {
          currentUser <- SecureSocial.currentUser(request)
          if (request.headers.get(IgnoreSession).isEmpty)
        } yield {
          logger.trace(s"currentUser: $currentUser")
          invokeAsUserAsync(currentUser.identityId.userId, currentUser.identityId.providerId, request)(f)
        }

        userResult.getOrElse(resultFromToken)
    }
  }

  private def ApiActionPermissions[A](p: BodyParser[A])(access: Permission)(f: ApiRequest[A] => Result) = {
    Action(p) {
      request =>
        SecureSocial.currentUser(request).find(_ => request.headers.get("CoreSpring-IgnoreSession").isEmpty).map(u => {
          invokeAsUser(u.identityId.userId, u.identityId.providerId, request) { request =>
            if (request.ctx.permission.has(access)) f(request)
            else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization(Some("your registered organization does not have acces to this request"))))
          }
        }).getOrElse(tokenFromRequest(request).fold(error => BadRequest(Json.toJson(error)), token =>
          oAuthProvider.getAuthorizationContext(token).fold(
            error => Forbidden(Json.toJson(error)).as(JSON),
            ctx => {
              ctx.permission.has(access)
              val result: SimpleResult = if (ctx.permission.has(access)) f(ApiRequest(ctx, request, token)).asInstanceOf[SimpleResult]
              else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization(Some("your registered organization does not have acces to this request"))))
              result
            })))
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
  def invokeAsUser[A](username: String, provider: String, request: Request[A])(f: ApiRequest[A] => Result): Result = {
    def orgId: Option[ObjectId] = ServiceLookup.userService.getUser(username, provider).map(_.org.orgId)

    val maybeOrg: Option[Organization] = orgId.map(ServiceLookup.orgService.findOneById).getOrElse(None)
    maybeOrg.map { org =>

      ServiceLookup.userService.getPermissions(username, org.id) match {
        case Failure(e) => BadRequest(e.message)
        case Success(p) => {
          val ctx = new AuthorizationContext(Option(username), org, p.getOrElse(Permission.Read), true)
          f(ApiRequest(ctx, request, ""))
        }
      }
    }.getOrElse(Forbidden(Json.toJson(ApiError.MissingCredentials)).as(JSON))
  }

  def invokeAsUserAsync[A](username: String, provider: String, request: Request[A])(f: ApiRequest[A] => Future[SimpleResult]): Future[SimpleResult] = {
    def orgId: Option[ObjectId] = ServiceLookup.userService.getUser(username, provider).map(_.org.orgId)

    val maybeOrg: Option[Organization] = orgId.map(ServiceLookup.orgService.findOneById).getOrElse(None)
    val result: Option[Future[SimpleResult]] = maybeOrg.map { org =>

      ServiceLookup.userService.getPermissions(username, org.id) match {
        case Failure(e) => Future.successful(BadRequest(e.message))
        case Success(p) => {
          val ctx = new AuthorizationContext(Option(username), org, p.getOrElse(Permission.Read), true)
          f(ApiRequest(ctx, request, ""))
        }
      }
    }
    result.getOrElse(Future.successful(Forbidden(Json.toJson(ApiError.MissingCredentials)).as(JSON)))
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

  def AsyncApiAction(f: ApiRequest[AnyContent] => Future[SimpleResult]): Action[AnyContent] = {
    AsyncApiAction(parse.anyContent)(f)
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
      request.ctx.user.map(block(_, request)).getOrElse(BadRequest(Json.toJson(ApiError.UserIsRequired)))
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

  def parsed[A](maybeJson: Option[JsValue], fn: (A => Result), noResult: Result = BadRequest("Bad Json"))(implicit format: Format[A]): Result = maybeJson match {
    case Some(json) => {
      play.api.libs.json.Json.fromJson[A](json) match {
        case JsSuccess(item, _) => fn(item)
        case _ => noResult
      }
    }
    case _ => noResult
  }
}
