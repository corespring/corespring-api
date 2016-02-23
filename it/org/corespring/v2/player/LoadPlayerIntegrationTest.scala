package org.corespring.v2.player

import org.bson.types.ObjectId
import org.corespring.container.client.V2PlayerConfig
import org.corespring.container.client.component.ComponentUrls
import org.corespring.container.client.controllers.apps.PageSourceService
import org.corespring.container.client.hooks.PlayerHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.processing.AssetPathProcessor
import org.corespring.container.components.model.Component
import org.corespring.container.components.processing.PlayerItemPreProcessor
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.scopes._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.specs2.mock.Mockito
import play.api.Mode.Mode
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.{ Configuration, GlobalSettings, Mode, Play }

import scala.concurrent.{ ExecutionContext, Future }

class LoadPlayerIntegrationTest
  extends IntegrationSpecification with Mockito {

  import org.corespring.container.client.controllers.apps.Player

  class MockPlayer(sessionId: String) extends Player {

    def showErrorInUi = false

    override def containerContext: ContainerExecutionContext = ContainerExecutionContext(ExecutionContext.global)

    override def hooks: PlayerHooks = new PlayerHooks {

      override def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {
        Right((Json.obj("id" -> sessionId), Json.obj()))
      }

      override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = ???
      override def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult = ???

      override def loadSessionAndItem(sessionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = ???

      override implicit def containerContext: ContainerExecutionContext = new ContainerExecutionContext(ExecutionContext.global)

      override def archiveCollectionId: String = main.archiveConfig.contentCollectionId.toString
    }

    override def urls: ComponentUrls = mock[ComponentUrls]

    override def components: Seq[Component] = Seq.empty

    override def itemPreProcessor: PlayerItemPreProcessor = {
      val m = mock[PlayerItemPreProcessor]
      m.preProcessItemForPlayer(any[JsValue]) answers { in => in.asInstanceOf[JsValue] }
    }

    override def mode: Mode = Mode.Test

    override def playerConfig: V2PlayerConfig = V2PlayerConfig(Configuration.empty)

    override def versionInfo: JsObject = Json.obj()

    override def assetPathProcessor: AssetPathProcessor = main.assetPathProcessor

    override def pageSourceService: PageSourceService = main.pageSourceService
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
      status(createSessionResult) === CREATED
      val mockResult = getMockResult(itemId)
      logger.debug(s"createSession result: ${headers(createSessionResult)}")
      locationNoQueryParams(createSessionResult) === locationNoQueryParams(mockResult)
    }

    "create session adds dateCreated field to the db document, and returns it in the session json" in new user_CreateSession() {
      status(createSessionResult) === CREATED
      val mockResult = getMockResult(itemId)
      val sessionId = v2SessionHelper.findSessionForItemId(itemId)
      val session = v2SessionHelper.findSession(sessionId.toString).get
      (session \ "dateCreated") must not be equalTo(null)

      val call = org.corespring.container.client.controllers.resources.routes.Session.loadItemAndSession(sessionId.toString)

      route(makeRequest(call))(writeable).map { result =>
        val json = contentAsJson(result)
        println(s" -> ${Json.stringify(json)}")
        (json \ "session" \ "dateCreated" \ "$date") match {
          case s: JsString => s.as[String].toString must not be equalTo("")
          case n: JsNumber => n.as[Long].toString must not be equalTo("")
          case _ => failure("Expected date to be string or number")
        }

      }.getOrElse(failure("should have been successful"))
    }

    "create session for access token" in new token_CreateSession() {
      status(createSessionResult) === CREATED
      val mockResult = getMockResult(itemId)
      logger.debug(s"createSession result: ${headers(createSessionResult)}")
      locationNoQueryParams(createSessionResult) === locationNoQueryParams(mockResult)
    }

    "fail - create session for client id + options query string" in
      new clientIdAndToken_queryString_CreateSession("Let me in") {
        status(createSessionResult) === UNAUTHORIZED
      }

    "fail - create session for client id + options query string, if token is missing 'expires'" in
      new clientIdAndToken_queryString_CreateSession(Json.stringify(Json.obj("itemId" -> "*"))) {
        status(createSessionResult) === UNAUTHORIZED
      }

    "create session for client id + options query string" in new clientIdAndToken_queryString_CreateSession(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
      status(createSessionResult) === CREATED
      val mockResult = getMockResult(itemId)
      logger.debug(s"createSession result: ${headers(createSessionResult)}")
      locationNoQueryParams(createSessionResult) === locationNoQueryParams(mockResult)
    }
  }

  private trait HasCreateSessionResult { self: HasItemId with RequestBuilder =>

    protected def global: GlobalSettings = Play.current.global

    lazy val createSessionResult: Future[SimpleResult] = {
      val player = global.getControllerInstance(classOf[Player])
      val createSession = player.createSessionForItem(itemId.toString)
      val request = makeRequest(Call("", ""))
      createSession(request)
    }
  }

  private class unknownIdentity_CreateSession extends HasCreateSessionResult with PlainRequestBuilder with orgWithAccessTokenAndItem {}

  private trait MockResultLoader { self: WithV2SessionHelper =>

    def getMockResult(itemId: VersionedId[ObjectId]) = {
      val sessionId = v2SessionHelper.findSessionForItemId(itemId)
      val mockPlayer = new MockPlayer(sessionId.toString)
      val mockResult = mockPlayer.createSessionForItem(itemId.toString)(FakeRequest("", ""))
      logger.debug(s"mockresult - headers: ${headers(mockResult)}")
      mockResult
    }
  }

  private class user_CreateSession
    extends userAndItem
    with HasCreateSessionResult
    with SessionRequestBuilder
    with SecureSocialHelper
    with WithV2SessionHelper
    with MockResultLoader {
    override lazy val usePreview = true
  }

  private class token_CreateSession
    extends orgWithAccessTokenAndItem
    with HasCreateSessionResult
    with TokenRequestBuilder
    with WithV2SessionHelper
    with MockResultLoader {
    override lazy val usePreview = false
  }

  private class clientIdAndToken_queryString_CreateSession(val playerToken: String, val skipDecryption: Boolean = true)
    extends clientIdAndPlayerToken
    with HasCreateSessionResult
    with IdAndPlayerTokenRequestBuilder
    with WithV2SessionHelper
    with MockResultLoader {
    override lazy val usePreview = false

  }
}
