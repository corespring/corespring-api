package org.corespring.v2.player

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.scopes._
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.mvc._
import play.api.{ GlobalSettings, Play }

import scala.concurrent.Future

class LoadPlayerIntegrationTest
  extends IntegrationSpecification with Mockito {

  import org.corespring.container.client.controllers.apps.Player

  "when I load the player with orgId and options" should {

    "fail to create session for unknown user" in new unknownIdentity_CreateSession() {
      status(createSessionResult) === UNAUTHORIZED
    }

    "create session for logged in user" in new user_CreateSession() {
      status(createSessionResult) === CREATED
    }

    "create session adds dateCreated field to the db document, and returns it in the session json" in new user_CreateSession() {
      status(createSessionResult) === CREATED
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

  private class user_CreateSession
    extends userAndItem
    with HasCreateSessionResult
    with SessionRequestBuilder
    with SecureSocialHelper
    with WithV2SessionHelper {
    override lazy val usePreview = true
  }

  private class token_CreateSession
    extends orgWithAccessTokenAndItem
    with HasCreateSessionResult
    with TokenRequestBuilder
    with WithV2SessionHelper {
    override lazy val usePreview = false
  }

  private class clientIdAndToken_queryString_CreateSession(val playerToken: String, val skipDecryption: Boolean = true)
    extends clientIdAndPlayerToken
    with HasCreateSessionResult
    with IdAndPlayerTokenRequestBuilder
    with WithV2SessionHelper {
    override lazy val usePreview = false
  }
}
