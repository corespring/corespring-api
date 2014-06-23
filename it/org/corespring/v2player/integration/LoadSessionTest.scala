package org.corespring.v2player.integration

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.V2SessionHelper
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2player.integration.scopes._
import play.api.http.{ Writeable, ContentTypeOf }
import play.api.libs.json.Json
import play.api.mvc.AnyContent

class LoadSessionTest extends IntegrationSpecification {

  "when I load a session" should {

    "fail for unknown user" in new unknownIdentity_loadSession() {
      status(result) ==== UNAUTHORIZED
    }

    "work for logged in user" in new user_loadSession() {
      status(result) ==== OK
    }

    "work for token" in new token_loadSession() {
      status(result) ==== OK
    }

    "fail for client id and bad options" in new clientId_loadSession("Let me in") {
      status(result) ==== UNAUTHORIZED
    }

    "work for client id and options" in new clientId_loadSession(Json.stringify(Json.toJson(PlayerOptions.ANYTHING))) {
      status(result) ==== OK
    }

  }

  trait loadSession extends { self: RequestBuilder with HasSessionId =>
    import org.corespring.container.client.controllers.resources.routes.Session

    lazy val result = {
      val call = Session.loadEverything(sessionId.toString)
      val request = makeRequest(call)
      logger.trace(s"load session make request: ${request.uri}")
      implicit val ct: ContentTypeOf[AnyContent] = new ContentTypeOf[AnyContent](None)
      val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
      route(request)(writeable).getOrElse(throw new RuntimeException("Error routing Session.loadEverything"))
    }
  }

  class unknownIdentity_loadSession extends loadSession with userWithItemAndSession with PlainRequestBuilder {}
  class user_loadSession extends loadSession with userWithItemAndSession with SessionRequestBuilder with SecureSocialHelpers {}
  class token_loadSession extends loadSession with orgWithAccessTokenItemAndSession with TokenRequestBuilder {}
  class clientId_loadSession(val options: String, val skipDecryption: Boolean = true) extends loadSession with clientIdAndOptions with IdAndOptionsRequestBuilder with HasSessionId {
    override lazy val sessionId: ObjectId = V2SessionHelper.create(itemId)

    override def after = {
      V2SessionHelper.delete(sessionId)
    }
  }

}
