package org.corespring.v2.api

import global.Global
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.orgWithAccessToken
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.missingRequiredField
import org.corespring.v2.errors.Field
import play.api.libs.json.{ JsNull, JsValue, Json }
import play.api.mvc.AnyContentAsJson
import play.api.test.{ FakeHeaders, FakeRequest }

class PlayerTokenApiIntegrationTest extends IntegrationSpecification {

  lazy val encryptionService = main.apiClientEncryptionService

  "PlayerTokenApi" should {
    s"$UNAUTHORIZED - create a player token" in new token_createPlayerToken {
      status(result) === UNAUTHORIZED
    }

    "create a player token" in new token_createPlayerToken {
      override val jsonBody = Json.obj("expires" -> 0)
      override val url = s"${call.url}?access_token=$accessToken"
      status(result) === OK

      val json = contentAsJson(result)
      (json \ "apiClient").as[String] === apiClient.clientId.toString
      val jsonResult = PlayerAccessSettings.permissiveRead(Json.obj("expires" -> 0))
      val settings = Json.stringify(Json.toJson(jsonResult.asOpt.get))
      val code = (json \ "playerToken").as[String]
      val decrypted = encryptionService.decrypt(apiClient, code)
      decrypted === Some(settings)
    }

    "return an error if the json can't be parsed as PlayerAccessSettings" in
      new token_createPlayerToken {
        override val jsonBody = Json.obj("itemId" -> "*")
        override val url = s"${call.url}?access_token=$accessToken"
        status(result) === BAD_REQUEST
        val json = contentAsJson(result)
        missingRequiredField(Field("expires", "number")).json === json
      }

  }

  trait token_createPlayerToken extends orgWithAccessToken {
    lazy val call = org.corespring.v2.api.routes.PlayerTokenApi.createPlayerToken()

    def url = call.url
    def jsonBody: JsValue = JsNull

    lazy val result = route(
      FakeRequest(call.method, url, FakeHeaders(), AnyContentAsJson(jsonBody))).getOrElse(throw new RuntimeException("Couldn't get a result"))
  }


}
