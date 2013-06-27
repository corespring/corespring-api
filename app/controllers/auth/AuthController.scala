package controllers.auth

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import api.ApiError
import org.bson.types.ObjectId
import securesocial.core.SecureSocial
import models.User
import play.api.data.validation._
import scala.Left
import scala.Some
import scala.Right
import play.api.data.validation.ValidationError
import web.controllers.ObjectIdParser
import common.log.PackageLogging


/**
 * A controller to handle the OAuth flow and consumer registration
 */

object AuthController extends Controller with SecureSocial with ObjectIdParser with PackageLogging{

  case class AccessTokenRequest(grant_type: String, client_id: String, client_secret: String, scope: Option[String])

  val registerInfo = Form(OAuthConstants.Organization -> text)

  /**
   * grantType: The OAuth flow (client_credentials is the only supported flow for now)
   * clientId: The client id
   * clientSignature: signature hashed by the client secret
   * scope: If specified this must be a username.  Using the scope parameter allows the caller to ghost a user.
   */
  val accessTokenForm = Form(
    mapping(
      OAuthConstants.GrantType -> optional(text),
      OAuthConstants.ClientId ->  text.verifying(validObjectIdConstraint),
      OAuthConstants.ClientSecret -> nonEmptyText,
      OAuthConstants.Scope -> optional(text)
    )((grantType,clientId,clientSecret,scope) =>
      AccessTokenRequest.apply(grantType.getOrElse(OAuthConstants.ClientCredentials),clientId,clientSecret,scope))
      (AccessTokenRequest.unapply(_).map((atrtuple => (Some(atrtuple._1),atrtuple._2,atrtuple._3,atrtuple._4))))
  )

  def validObjectIdConstraint : Constraint[String] = {
    def validOid(s:String) = objectId(s).isDefined
    Constraint[String]({
     s : String => s match {
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
          val username = request.user.id.id
          val orgId = new ObjectId(orgStr)
          User.getUser(username) match {
            case Some(user) => if(user.org.orgId == orgId && (user.org.pval&Permission.Write.value) == Permission.Write.value){
              try {
                OAuthProvider.register(orgId).fold(
                  error => BadRequest(Json.toJson(error)),
                  client => Ok(Json.toJson(Map(OAuthConstants.ClientId -> client.clientId.toString, OAuthConstants.ClientSecret -> client.clientSecret)))
                )
              } catch {
                case ex: IllegalArgumentException => BadRequest(Json.toJson(ApiError.MissingOrganization))
              }
            }else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
            case None => {
              Logger.error("user was authorized but does not exist!")
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
          OAuthProvider.getAccessToken(params.grant_type, params.client_id, params.client_secret, params.scope) match {
            case Right(token) =>
              val result = Map(OAuthConstants.AccessToken -> token.tokenId) ++ token.scope.map(OAuthConstants.Scope -> _)
              Ok(Json.toJson(result))
            case Left(error) => Forbidden(Json.toJson(error))
          }
      ).as(JSON)
  }
}
