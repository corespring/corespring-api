package org.corespring.v2player.integration

import org.corespring.it.IntegrationSpecification
import org.corespring.test.SecureSocialHelpers
import org.corespring.v2player.integration.scopes._
import play.api.http.{ Writeable, ContentTypeOf }
import play.api.mvc.AnyContent

class LoadSessionTest extends IntegrationSpecification {

  "when I load a session" should {

    "fail for unknown user" in new unknownIdentity_loadSession() {
      status(result) ==== UNAUTHORIZED
    }

    "work for logged in user" in new user_loadSession() {
      status(result) ==== OK
    }
  }

  trait loadSession extends { self: RequestBuilder with HasSessionId =>
    import org.corespring.container.client.controllers.resources.routes.Session

    lazy val result = {
      val call = Session.loadEverything(sessionId.toString)
      val request = makeRequest(call)
      implicit val ct: ContentTypeOf[AnyContent] = new ContentTypeOf[AnyContent](None)
      val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
      route(request)(writeable).getOrElse(throw new RuntimeException("Error routing Session.loadEverything"))
    }
  }

  class unknownIdentity_loadSession extends loadSession with userWithItemAndSession with PlainRequestBuilder {}
  class user_loadSession extends loadSession with userWithItemAndSession with SessionRequestBuilder with SecureSocialHelpers {}

}
