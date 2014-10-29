package org.corespring.v2.player

import org.bson.types.ObjectId
import org.corespring.container.client.component.ComponentUrls
import org.corespring.container.client.hooks.PlayerHooks
import org.corespring.container.components.model.Component
import org.corespring.container.components.processing.PlayerItemPreProcessor
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.V2SessionHelper
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.player.scopes._
import org.specs2.mock.Mockito
import play.api.Mode.Mode
import play.api.Mode.Mode
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.{ Mode, GlobalSettings, Play }

import scala.concurrent.{ ExecutionContext, Future }

class LoadPlayerTest
  extends IntegrationSpecification with Mockito {

  import org.corespring.container.client.controllers.apps.Player

  class MockPlayer(sessionId: String) extends Player {

    def showErrorInUi = false

    override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

    override def hooks: PlayerHooks = new PlayerHooks {

      override def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), String]] = Future {
        Right(sessionId)
      }

      override def loadItem(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = ???

      override def loadSessionAndItem(sessionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = ???
    }

    override def urls: ComponentUrls = mock[ComponentUrls]

    override def components: Seq[Component] = Seq.empty

    override def itemPreProcessor: PlayerItemPreProcessor = ???

    override def mode: Mode = Mode.Test
  }

  def getMockResult(itemId: VersionedId[ObjectId], collection: String) = {
    val sessionId = V2SessionHelper.findSessionForItemId(itemId, collection)
    val mockPlayer = new MockPlayer(sessionId.toString)
    val mockResult = mockPlayer.createSessionForItem(itemId.toString)(FakeRequest("", ""))
    logger.debug(s"mockresult - headers: ${headers(mockResult)}")
    mockResult
  }

  "when I load the player with orgId and options" should {

    "fail to create session for unknown user" in new unknownIdentity_CreateSession() {
      status(createSessionResult) === UNAUTHORIZED
    }

    def locationNoQueryParams(of: Future[SimpleResult]): String = {
      headers(of).get(LOCATION).map(l =>
        if (l.indexOf("?") == -1) l else l.split("\\?")(0)).getOrElse(throw new RuntimeException("no location in header"))
    }

    "create session for logged in user" in new user_CreateSession() {
      status(createSessionResult) === SEE_OTHER
      val mockResult = getMockResult(itemId, "v2.itemSessions_preview")
      logger.debug(s"createSession result: ${headers(createSessionResult)}")
      locationNoQueryParams(createSessionResult) === locationNoQueryParams(mockResult)
    }

    "create session for access token" in new token_CreateSession() {
      status(createSessionResult) === SEE_OTHER
      val mockResult = getMockResult(itemId, "v2.itemSessions")
      logger.debug(s"createSession result: ${headers(createSessionResult)}")
      locationNoQueryParams(createSessionResult) === locationNoQueryParams(mockResult)
    }

    "fail - create session for client id + options query string" in new clientIdAndToken_queryString_CreateSession("Let me in") {
      status(createSessionResult) === UNAUTHORIZED
    }

    "create session for client id + options query string" in new clientIdAndToken_queryString_CreateSession(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
      status(createSessionResult) === SEE_OTHER
      val mockResult = getMockResult(itemId, "v2.itemSessions")
      logger.debug(s"createSession result: ${headers(createSessionResult)}")
      locationNoQueryParams(createSessionResult) === locationNoQueryParams(mockResult)
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
  class clientIdAndToken_queryString_CreateSession(val playerToken: String, val skipDecryption: Boolean = true) extends clientIdAndPlayerToken with HasCreateSessionResult with IdAndPlayerTokenRequestBuilder {}
}
