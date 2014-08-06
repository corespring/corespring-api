package org.corespring.v2.player

import org.corespring.container.client.controllers.PlayerLauncher
import org.corespring.it.IntegrationSpecification
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.player.scopes.{ HasItemId, IdAndOptionsRequestBuilder, RequestBuilder, clientIdAndOptions }
import play.api.libs.json.Json
import play.api.mvc.{ Call, SimpleResult }
import play.api.{ GlobalSettings, Play }

import scala.concurrent.Future

class LoadPlayerJsTest extends IntegrationSpecification {

  "launch player js" should {

    "load player js with client id + options query string sets session" in new queryString_loadJs(Json.stringify(Json.toJson(PlayerOptions.ANYTHING))) {
      status(playerJsResult) === OK
      contentAsString(playerJsResult).contains(s"apiClient = '${apiClient.clientId}'")
      contentAsString(playerJsResult).contains(s"options = '$options'")
    }

    """load player js with client id + options
      query string sets session""" in new queryString_loadJs(
      Json.stringify(Json.toJson(new PlayerOptions(itemId = "*", secure = true)))) {
      status(playerJsResult) === OK
      contentAsString(playerJsResult).contains(s"apiClient = '${apiClient.clientId}'")
      contentAsString(playerJsResult).contains(s"options = '$options'")
    }
  }

  trait HasPlayerJsResult { self: HasItemId with RequestBuilder =>

    protected def global: GlobalSettings = Play.current.global

    lazy val playerJsResult: Future[SimpleResult] = {
      val launcher = global.getControllerInstance(classOf[PlayerLauncher])
      val request = makeRequest(Call("", ""))
      launcher.playerJs()(request)
    }
  }

  class queryString_loadJs(val options: String, val skipDecryption: Boolean = true) extends clientIdAndOptions with HasPlayerJsResult with IdAndOptionsRequestBuilder {}

}
