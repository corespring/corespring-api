package controllers.auth

import play.api.libs.json.{JsValue, Reads}

case class AccessTokenForm(grant_type: String, client_id: String, client_signature: Option[String], algorithm:String, scope: Option[String])
object AccessTokenForm{
  implicit object Reads extends Reads[AccessTokenForm]{
    def reads(json:JsValue):AccessTokenForm = {
      AccessTokenForm(
        (json \ "grant_type").asOpt[String].getOrElse(OAuthConstants.ClientCredentials),
        (json \ "client_id").as[String],
        (json \ "client_signature").asOpt[String],
        (json \ "algorithm").asOpt[String].getOrElse("HmacSha1"),
        (json \ "scope").asOpt[String]
      )
    }
  }
}
