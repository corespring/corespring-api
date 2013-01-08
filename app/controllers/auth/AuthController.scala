package controllers.auth

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Results, Result, Action, Controller}
import api.ApiError
import org.bson.types.ObjectId
import securesocial.core.SecureSocial
import models.User
import controllers.Log


/**
 * A controller to handle the OAuth flow and consumer registration
 */

object AuthController extends Controller with SecureSocial{

  case class AccessTokenRequest(grant_type: String, client_id: String, client_secret: String, scope: Option[String])

  val registerInfo = Form(OAuthConstants.Organization -> text)

  val accessTokenForm = Form(
    mapping(
      OAuthConstants.GrantType -> nonEmptyText,
      OAuthConstants.ClientId -> nonEmptyText,
      OAuthConstants.ClientSecret -> nonEmptyText,
      OAuthConstants.Scope -> optional(text)
    )(AccessTokenRequest.apply)(AccessTokenRequest.unapply)
  )

  def register = SecuredAction() { implicit request =>
      registerInfo.bindFromRequest().value.map { orgStr =>
          val username = request.user.id.id
          val orgId = new ObjectId(orgStr)
          User.getUser(username) match {
            case Some(user) => if(user.orgs.exists(uo => uo.orgId == orgId && (uo.pval&Permission.Write.value) == Permission.Write.value)){
              try {
                OAuthProvider.register(orgId,username).fold(
                  error => BadRequest(Json.toJson(error)),
                  client => Ok(Json.toJson(Map(OAuthConstants.ClientId -> client.clientId.toString, OAuthConstants.ClientSecret -> client.clientSecret)))
                )
              } catch {
                case ex: IllegalArgumentException => BadRequest(Json.toJson(ApiError.MissingOrganization))
              }
            }else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
            case None => {
              Log.e("user was authorized but does not exist!")
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
