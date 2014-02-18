package org.corespring.v2player.integration

import org.corespring.common.encryption.AESCrypto
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2player.integration.actionBuilders.access.{ V2PlayerCookieKeys, PlayerOptions }
import org.corespring.v2player.integration.scopes.data
import play.api.libs.json.Json
import play.api.mvc.SimpleResult
import play.api.test.FakeRequest
import play.api.{ Play, GlobalSettings }
import scala.concurrent.Future

class LaunchPlayerJsSecureModeTest extends IntegrationSpecification {

  "launch player js" should {
    "launch in secure mode" in new LoadJs(true) {
      maybeSecure(jsResult) match {
        case Some(true) => success
        case _ => failure("error")
      }
    }

    "launch in non secure mode" in new LoadJs(false) {
      maybeSecure(jsResult) match {
        case Some(false) => success
        case _ => failure("error")
      }
    }
  }

  class LoadJs(secure: Boolean) extends data {

    protected def global: GlobalSettings = Play.current.global

    lazy val jsResult = {
      val url = urlWithEncryptedOptions("", apiClient)
      val launcher = global.getControllerInstance(classOf[org.corespring.container.client.controllers.PlayerLauncher])
      launcher.playerJs(FakeRequest(GET, url))
    }

    def urlWithEncryptedOptions(call: String, apiClient: ApiClient, options: PlayerOptions = PlayerOptions.ANYTHING) = {
      val playerOptions = PlayerOptions("*", Some("*"), secure)
      val optionString = Json.stringify(Json.toJson(playerOptions))
      val encrypted = AESCrypto.encrypt(optionString, apiClient.clientSecret)
      s"${call}?apiClient=${apiClient.clientId}&options=$encrypted"
    }

    def maybeSecure(r: Future[SimpleResult]): Option[Boolean] = {
      session(r).get(V2PlayerCookieKeys.renderOptions).map {
        optionString =>
          Json.parse(optionString).asOpt[PlayerOptions].map(_.secure)
      }.flatten
    }
  }

}
