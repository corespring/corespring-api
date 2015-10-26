package org.corespring.v2.player

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.scopes._
import org.corespring.v2.auth.models.PlayerAccessSettings
import play.api.http.{ ContentTypeOf, Writeable }
import play.api.libs.json.Json
import play.api.mvc.AnyContent

class LoadSessionIntegrationTest extends IntegrationSpecification {

  "when I load a session" should {

    "fail for unknown user" in new unknownIdentity_loadSession() {
      status(result) ==== UNAUTHORIZED
    }

    "work for logged in user" in new user_loadSession() {
      println(contentAsString(result))
      status(result) ==== OK
    }

    "work for token" in new token_loadSession() {
      status(result) ==== OK
    }

    "fail for client id and bad options" in new clientId_loadSession("Let me in") {
      status(result) ==== UNAUTHORIZED
    }

    "fail - create session for client id + options query string, if token is missing 'expires'" in
      new clientId_loadSession(Json.stringify(Json.obj("itemId" -> "*"))) {
        status(result) === UNAUTHORIZED
      }

    "work for client id and options" in new clientId_loadSession(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
      status(result) ==== OK
    }

  }

  trait loadSession extends { self: RequestBuilder with HasSessionId =>
    import org.corespring.container.client.controllers.resources.routes.Session

    lazy val result = {
      val call = Session.loadItemAndSession(sessionId.toString)
      val request = makeRequest(call)
      logger.trace(s"load session make request: ${request.uri}")
      implicit val ct: ContentTypeOf[AnyContent] = new ContentTypeOf[AnyContent](None)
      val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
      route(request)(writeable).getOrElse(throw new RuntimeException("Error routing Session.loadEverything"))
    }
  }

  class unknownIdentity_loadSession extends loadSession with userWithItemAndSession with PlainRequestBuilder

  class user_loadSession extends loadSession with userWithItemAndSession with SessionRequestBuilder with SecureSocialHelper {
    override lazy val usePreview = true
  }

  class token_loadSession extends loadSession with orgWithAccessTokenItemAndSession with TokenRequestBuilder

  class clientId_loadSession(val playerToken: String, val skipDecryption: Boolean = true) extends loadSession with clientIdAndPlayerToken with IdAndPlayerTokenRequestBuilder with HasSessionId with WithV2SessionHelper {
    override lazy val sessionId: ObjectId = v2SessionHelper.create(itemId)

    override def after = {
      v2SessionHelper.delete(sessionId)
    }
  }

}
