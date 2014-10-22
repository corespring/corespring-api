package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.test.helpers.models.V2SessionHelper
import org.corespring.v2.player.scopes.orgWithAccessToken
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.{ FakeHeaders, FakeRequest }

class ExternalModelLaunchApiTest extends IntegrationSpecification {

  trait launchExternalAndLoadPlayer extends orgWithAccessToken {

    lazy val launchCall = org.corespring.v2.api.routes.ExternalModelLaunchApi.buildExternalLaunchSession()
    val json = Json.obj(
      "model" -> Json.obj(
        "xhtml" -> "<h1>Hello World</h1>",
        "components" -> Json.obj()),
      "accessSettings" -> Json.obj(
        "expires" -> 0))
    val launchResult = route(FakeRequest(launchCall.method, s"${launchCall.url}?access_token=$accessToken", FakeHeaders(), AnyContentAsJson(json))).get
    lazy val launchJson = contentAsJson(launchResult)
    lazy val sessionId = (launchJson \ "sessionId").as[String]
    lazy val playerToken = (launchJson \ "playerToken").as[String]

    lazy val playerCall = org.corespring.container.client.controllers.apps.routes.ProdHtmlPlayer.config(sessionId)

    lazy val playerResult = route(
      FakeRequest(
        playerCall.method,
        s"${playerCall.url}?apiClient=${apiClient.clientId}&playerToken=$playerToken")).get

    override def after = {
      super.after
      V2SessionHelper.delete(new ObjectId(sessionId))
    }

  }

  "ExternalModelLaunchApi" should {
    "allow you to create an item and load it in the player" in new launchExternalAndLoadPlayer {
      status(playerResult) === OK
      contentAsString(playerResult).contains("<h1>Hello World</h1>") === true
    }
  }
}
