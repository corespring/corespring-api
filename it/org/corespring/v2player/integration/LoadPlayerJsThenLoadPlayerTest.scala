package org.corespring.v2player.integration

import org.corespring.common.encryption.AESCrypto
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.scopes.orgWithAccessTokenAndItem
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.{ GlobalSettings, Play }
import scala.concurrent.Future

class LoadPlayerJsThenLoadPlayerTest
  extends IntegrationSpecification {

  import org.corespring.container.client.controllers.PlayerLauncher
  import org.corespring.container.client.controllers.apps.Player

  "when I load the player js with orgId and options" should {

    "load js with no errors" in new LoadJsAndCreateSession("js no errors") {
      contentAsString(jsResult).contains("exports.hasErrors = false;") === true
    }

    "fail if i don't pass in the session" in new LoadJsAndCreateSession("fail 1", false) {
      contentAsString(jsResult).contains("exports.hasErrors = false;") === true
      status(createSessionResult) === SEE_OTHER
      headers(createSessionResult).get("Location") === Some("/login")
    }

    "allow me to create a session" in new LoadJsAndCreateSession("allow 1", true) {
      status(createSessionResult) === SEE_OTHER
      headers(createSessionResult).get("Location").get.contains("/player")
    }

    "allow me to create a session and load player" in new LoadJsAndCreateSessionAndLoadPlayer("allow 1", true) {
      status(loadPlayerResult) === OK
      val expected = scala.io.Source.fromURL(Play.resource("/container-client/player.html").get).getLines.mkString("\n")
      contentAsString(loadPlayerResult) === expected
    }
  }

  class LoadJsAndCreateSession(name: String, addCookies: Boolean = false) extends orgWithAccessTokenAndItem {

    protected def global: GlobalSettings = Play.current.global

    lazy val jsResult = {
      val url = urlWithEncryptedOptions("", apiClient)
      val launcher = global.getControllerInstance(classOf[PlayerLauncher])
      launcher.playerJs(FakeRequest(GET, url))
    }

    lazy val createSessionResult: Future[SimpleResult] = {
      val player = global.getControllerInstance(classOf[Player])
      val createSession = player.createSessionForItem(itemId.toString)
      val jsCookies = if (addCookies) cookies(jsResult) else Cookies(None)
      createSession(FakeRequest("", "").withCookies(jsCookies.toSeq: _*))
    }

    def urlWithEncryptedOptions(call: String, apiClient: ApiClient, options: PlayerOptions = PlayerOptions.ANYTHING) = {
      val options = Json.stringify(Json.toJson(PlayerOptions.ANYTHING))
      val encrypted = AESCrypto.encrypt(options, apiClient.clientSecret)
      s"${call}?apiClient=${apiClient.clientId}&options=$encrypted"
    }
  }

  class LoadJsAndCreateSessionAndLoadPlayer(name: String, addCookies: Boolean) extends LoadJsAndCreateSession(name, addCookies) {
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
  }
}
