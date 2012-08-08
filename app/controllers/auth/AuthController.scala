package controllers.auth

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Results, Result, Action, Controller}
import api.ApiError
import org.bson.types.ObjectId


/**
 * A controller to handle the OAuth flow and consumer registration
 */

object AuthController extends Controller {

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

  def register = Action {
    implicit request =>
      registerInfo.bindFromRequest().value.map {
        orgStr =>
          try {
            OAuthProvider.register(new ObjectId(orgStr)).fold(
              error => BadRequest(Json.toJson(error)),
              client => Ok(Json.toJson(Map(OAuthConstants.ClientId -> client.clientId.toString, OAuthConstants.ClientSecret -> client.clientSecret)))
            )
          } catch {
            case ex: IllegalArgumentException => BadRequest(Json.toJson(ApiError.MissingOrganization))
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
