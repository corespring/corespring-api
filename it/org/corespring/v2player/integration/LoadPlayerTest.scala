package org.corespring.v2player.integration

import org.corespring.common.encryption.AESCrypto
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2player.integration.scopes.orgWithAccessTokenAndItem
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.{ GlobalSettings, Play }

import scala.concurrent.Future

class LoadPlayerTest
  extends IntegrationSpecification {

  import org.corespring.container.client.controllers.apps.Player

  "when I load the player with orgId and options" should {

    /*"load with no errors" in new LoadJsAndCreateSession("js no errors") {
      contentAsString(jsResult).contains("exports.hasErrors = false;") === true
    }

    "fail if i don't pass credentials to the query params" in new LoadJsAndCreateSession("fail 1", false) {
      status(createSessionResult) === SEE_OTHER
      headers(createSessionResult).get("Location") === Some("/login")
    }
    */

    "allow me to create a session" in new LoadJsAndCreateSession("allow 1", true) {
      status(createSessionResult) === SEE_OTHER
      logger.debug(s"status ${status(createSessionResult)}")
      logger.debug(s" headers: ${headers(createSessionResult).toString}")
      headers(createSessionResult).get("Location") === Some("/player")
    }

    /*
    "allow me to create a session and load player" in new LoadJsAndCreateSessionAndLoadPlayer("allow 1", true) {
      status(loadPlayerResult) === OK
      val expected = scala.io.Source.fromURL(Play.resource("/container-client/player.dev.html").get).getLines.mkString("\n")
      contentAsString(loadPlayerResult) === expected
    }*/
  }

  class LoadJsAndCreateSession(name: String, addCredentials: Boolean = false) extends orgWithAccessTokenAndItem {

    protected def global: GlobalSettings = Play.current.global

    lazy val createSessionResult: Future[SimpleResult] = {
      val player = global.getControllerInstance(classOf[Player])
      val createSession = player.createSessionForItem(itemId.toString)
      val url = if (addCredentials) urlWithEncryptedOptions("", apiClient) else ""
      logger.debug(s"calling create session with: $url")
      createSession(FakeRequest("", url))
    }

    def urlWithEncryptedOptions(url: String, apiClient: ApiClient, options: PlayerOptions = PlayerOptions.ANYTHING) = {
      val options = Json.stringify(Json.toJson(PlayerOptions.ANYTHING))
      val encrypted = AESCrypto.encrypt(options, apiClient.clientSecret)
      s"${url}?apiClient=${apiClient.clientId}&options=$encrypted"
    }
  }

  /*class LoadJsAndCreateSessionAndLoadPlayer(name: String, addCookies: Boolean) extends LoadJsAndCreateSession(name, addCookies) {
    lazy val loadPlayerResult: Future[SimpleResult] = {
      val redirect = headers(createSessionResult).get("Location").getOrElse(throw new RuntimeException("Error getting location"))
      //TODO: We are avoiding calling `route` here - is there a nicer way to get the Action?
      val Regex = """/v2/player/session/(.*?)/.*""".r
      val Regex(id) = redirect
      logger.debug(s" id is: $id")
      val c = global.getControllerInstance(classOf[Player])
      val out = c.loadPlayerForSession(id)(FakeRequest(GET, redirect).withCookies(cookies(jsResult).toSeq: _*))
      out
    }
  }*/
}
