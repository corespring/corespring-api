package org.corespring.v2player.integration

import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.container.client.component.ComponentUrls
import org.corespring.container.client.hooks.PlayerHooks
import org.corespring.container.components.model.Component
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.V2SessionHelper
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2player.integration.scopes._
import org.specs2.mock.Mockito
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.{ GlobalSettings, Play }

import scala.concurrent.{ ExecutionContext, Future }

class LoadPlayerTest
  extends IntegrationSpecification with Mockito {

  import org.corespring.container.client.controllers.apps.Player

  class MockPlayer(sessionId: String) extends Player {
    override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

    override def hooks: PlayerHooks = new PlayerHooks {
      override def loadPlayerForSession(sessionId: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = ???

      override def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), String]] = Future {
        Right(sessionId)
      }

      override def loadItem(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = ???
    }

    override def urls: ComponentUrls = mock[ComponentUrls]

    override def components: Seq[Component] = Seq.empty
  }

  def getMockResult(itemId: VersionedId[ObjectId]) = {
    val sessionId = V2SessionHelper.findSessionForItemId(itemId)
    val mockPlayer = new MockPlayer(sessionId.toString)
    val mockResult = mockPlayer.createSessionForItem(itemId.toString)(FakeRequest("", ""))
    logger.debug(s"mockresult - headers: ${headers(mockResult)}")
    mockResult
  }

  "when I load the player with orgId and options" should {

    "fail to create session for unknown user" in new unknownIdentity_CreateSession() {
      status(createSessionResult) === UNAUTHORIZED
    }

    "create session for logged in user" in new user_CreateSession() {
      status(createSessionResult) === SEE_OTHER
      val mockResult = getMockResult(itemId)
      logger.debug(s"createSession result: ${headers(createSessionResult)}")
      headers(createSessionResult) === headers(mockResult)
    }

    "create session for access token" in new token_CreateSession() {
      status(createSessionResult) === SEE_OTHER
      val mockResult = getMockResult(itemId)
      logger.debug(s"createSession result: ${headers(createSessionResult)}")
      headers(createSessionResult) === headers(mockResult)
    }

    "fail - create session for client id + options query string" in new clientIdAndOpts_queryString_CreateSession("Let me in") {
      status(createSessionResult) === UNAUTHORIZED
    }

    "create session for client id + options query string" in new clientIdAndOpts_queryString_CreateSession(Json.stringify(Json.toJson(PlayerOptions.ANYTHING))) {
      status(createSessionResult) === SEE_OTHER
      val mockResult = getMockResult(itemId)
      logger.debug(s"createSession result: ${headers(createSessionResult)}")
      headers(createSessionResult) === headers(mockResult)
    }
  }

  trait HasCreateSessionResult { self: HasItemId with RequestBuilder =>

    protected def global: GlobalSettings = Play.current.global

    lazy val createSessionResult: Future[SimpleResult] = {
      val player = global.getControllerInstance(classOf[Player])
      val createSession = player.createSessionForItem(itemId.toString)
      val request = makeRequest(Call("", ""))
      createSession(request)
    }

  }

  class unknownIdentity_CreateSession extends HasCreateSessionResult with PlainRequestBuilder with orgWithAccessTokenAndItem {}

  class user_CreateSession extends userAndItem with HasCreateSessionResult with SessionRequestBuilder with SecureSocialHelpers {}
  class token_CreateSession extends orgWithAccessTokenAndItem with HasCreateSessionResult with TokenRequestBuilder {}
  class clientIdAndOpts_queryString_CreateSession(val options: String, val skipDecryption: Boolean = true) extends clientIdAndOptions with HasCreateSessionResult with IdAndOptionsRequestBuilder {}

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
