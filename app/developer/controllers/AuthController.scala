package developer.controllers

import com.mongodb.casbah.Imports._
import common.db.ObjectIdParser
import org.corespring.models.auth.Permission
import org.corespring.platform.core.controllers.auth.OAuthProvider
import org.corespring.services.UserService
import org.corespring.web.api.v1.errors.ApiError
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation._
import play.api.libs.json.{ JsBoolean, JsObject, Json }
import play.api.mvc.{ Action, Controller }
import securesocial.core.SecureSocial

import scalaz.{ Failure, Success }

class AuthController(userService: UserService,
  oAuthProvider: OAuthProvider)
  extends Controller
  with SecureSocial
  with ObjectIdParser {

  case class AccessTokenRequest(grant_type: String, client_id: String, client_secret: String, scope: Option[String])

  val registerInfo = Form(OAuthConstants.Organization -> text)

  private lazy val logger = Logger(classOf[AuthController])

  /**
   * grantType: The OAuth flow (client_credentials is the only supported flow for now)
   * clientId: The client id
   * clientSignature: signature hashed by the client secret
   * scope: If specified this must be a username.  Using the scope parameter allows the caller to ghost a user.
   */
  val accessTokenForm = Form(
    mapping(
      OAuthConstants.GrantType -> optional(text),
      OAuthConstants.ClientId -> text.verifying(validObjectIdConstraint),
      OAuthConstants.ClientSecret -> nonEmptyText,
      OAuthConstants.Scope -> optional(text))((grantType, clientId, clientSecret, scope) =>
        AccessTokenRequest.apply(grantType.getOrElse(OAuthConstants.ClientCredentials), clientId, clientSecret, scope))(AccessTokenRequest.unapply(_).map((atrtuple => (Some(atrtuple._1), atrtuple._2, atrtuple._3, atrtuple._4)))))

  def validObjectIdConstraint: Constraint[String] = {
    def validOid(s: String) = objectId(s).isDefined
    Constraint[String]({
      s: String =>
        s match {
          case _ if s.length == 0 => Invalid(ValidationError("empty"))
          case oid if !validOid(oid) => Invalid(ValidationError("Invalid object id"))
          case _ => Valid
        }
    })
  }

  /**
   * {
   *   grant_type: client_credentials
   *   algorithm: HmacSha1
   *   client_id: [client]
   *
   * }
   * @return
   */
  def register = SecuredAction { implicit request =>
    registerInfo.bindFromRequest().value.map { orgStr =>
      val username = request.user.identityId.userId
      val orgId = new ObjectId(orgStr)
      userService.getUser(username) match {
        case Some(user) => if (user.org.orgId == orgId && (user.org.pval & Permission.Write.value) == Permission.Write.value) {
          try {
            oAuthProvider.createApiClient(orgId).fold(
              error => BadRequest(Json.toJson(error)),
              client => Ok(Json.toJson(Map(OAuthConstants.ClientId -> client.clientId.toString, OAuthConstants.ClientSecret -> client.clientSecret))))
          } catch {
            case ex: IllegalArgumentException => BadRequest(Json.toJson(ApiError.MissingOrganization))
          }
        } else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
        case None => {
          logger.error("user was authorized but does not exist!")
          InternalServerError
        }
      }
    }.getOrElse(BadRequest(Json.toJson(ApiError.MissingOrganization))).as(JSON)
  }

  def getAccessToken = Action {
    implicit request =>
      accessTokenForm.bindFromRequest.fold(
        errors => BadRequest(errors.errorsAsJson),
        params =>
          oAuthProvider.getAccessToken(params.grant_type, params.client_id, params.client_secret, params.scope) match {
            case Success(token) =>
              logger.debug(s"getAccessToken returns token: ${token.tokenId}")
              val result = Map(OAuthConstants.AccessToken -> token.tokenId) ++ token.scope.map(OAuthConstants.Scope -> _)
              Ok(Json.toJson(result))
            case Failure(error) => Forbidden(Json.toJson(error))
          }).as(JSON)
  }

  def isValid(token: String) = Action {
    oAuthProvider.getAuthorizationContext(token) match {
      case Success(context) => Ok(JsObject(Seq("valid" -> JsBoolean(true))))
      case Failure(error) => Forbidden(Json.toJson(error))
    }
  }

}