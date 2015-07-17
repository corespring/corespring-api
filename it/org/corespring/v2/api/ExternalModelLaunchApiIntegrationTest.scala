package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.test.helpers.models.V2SessionHelper
import org.corespring.v2.player.scopes.orgWithAccessToken
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.{ FakeHeaders, FakeRequest }

class ExternalModelLaunchApiIntegrationTest extends IntegrationSpecification {

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

    lazy val playerCall = org.corespring.container.client.controllers.apps.routes.Player.load(sessionId)
    lazy val loadItemAndSessionCall = org.corespring.container.client.controllers.resources.routes.Session.loadItemAndSession(sessionId)

    lazy val playerResult = route(
      FakeRequest(
        playerCall.method,
        s"${playerCall.url}?apiClient=${apiClient.clientId}&playerToken=$playerToken")).get

    lazy val loadItemAndSessionResult = route(
      FakeRequest(
        loadItemAndSessionCall.method,
        s"${loadItemAndSessionCall.url}?apiClient=${apiClient.clientId}&playerToken=$playerToken")).get

    override def after = {
      super.after
      V2SessionHelper.delete(new ObjectId(sessionId))
    }

  }

  "ExternalModelLaunchApi" should {
    "allow you to create an item and load it in the player" in new launchExternalAndLoadPlayer {
      status(playerResult) === OK
      println(contentAsString(loadItemAndSessionResult))
      (contentAsJson(loadItemAndSessionResult) \ "item" \ "xhtml").as[String] === "<h1>Hello World</h1>"
    }
  }
}
