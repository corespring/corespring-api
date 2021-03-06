package org.corespring.v2.player

import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.{ HasItemId, IdAndPlayerTokenRequestBuilder, RequestBuilder, clientIdAndPlayerToken }
import org.corespring.v2.auth.identifiers.PlayerTokenIdentity.Keys
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.warnings.Warnings.deprecatedQueryStringParameter
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.{ FakeHeaders, FakeRequest }
import play.api.{ GlobalSettings, Play }

import scala.concurrent.Future

class LoadPlayerJsIntegrationTest extends IntegrationSpecification {

  val anythingJson = Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))
  "launch player js" should {

    "load player js with client id + playerToken query string sets session" in
      new queryString_loadJs(anythingJson) {
        status(playerJsResult) === OK
      }

    """load player js with client id + playerToken
      query string sets session""" in new queryString_loadJs(
      Json.stringify(Json.toJson(new PlayerAccessSettings(itemId = "*", secure = true)))) {
      status(playerJsResult) === OK
    }

    """load player js returns a warning when you load using 'options'""" in
      new queryStringWithOptions_loadJs(anythingJson) {

        status(playerJsResult) === OK
        val js = contentAsString(playerJsResult)
        val warning = deprecatedQueryStringParameter(Keys.options, Keys.playerToken)
        js.contains(warning.code) === true
        js.contains(warning.message) === true
      }

  }

  trait HasPlayerJsResult { self: HasItemId with RequestBuilder =>

    protected def global: GlobalSettings = Play.current.global

    lazy val playerJsResult: Future[SimpleResult] = {
      import org.corespring.container.client.controllers.launcher.player.PlayerLauncher
      val launcher = global.getControllerInstance(classOf[PlayerLauncher])
      val request = makeRequest(Call("", ""))
      launcher.playerJs()(request)
    }
  }

  trait IdAndOptionsRequestBuilder extends RequestBuilder { self: clientIdAndPlayerToken =>

    def skipDecryption: Boolean

    override def makeRequest[A <: AnyContent](call: Call, body: A = AnyContentAsEmpty): Request[A] = {
      val basicUrl = s"${call.url}?${Keys.apiClient}=$clientId&${Keys.options}=$playerToken"
      val finalUrl = if (skipDecryption) s"$basicUrl&${Keys.skipDecryption}=true" else basicUrl
      FakeRequest(call.method, finalUrl, FakeHeaders(), body)
    }
  }

  class queryString_loadJs(val playerToken: String, val skipDecryption: Boolean = true)
    extends clientIdAndPlayerToken
    with HasPlayerJsResult
    with IdAndPlayerTokenRequestBuilder

  class queryStringWithOptions_loadJs(val playerToken: String, val skipDecryption: Boolean = true)
    extends clientIdAndPlayerToken
    with HasPlayerJsResult
    with IdAndOptionsRequestBuilder

}
