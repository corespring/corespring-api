package org.corespring.api.tracking

import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.Call

class ApiTrackingLoggerTest extends Specification with Mockito {

  import org.corespring.container.client.controllers.apps.routes._

  class scope extends Scope {
    val t = new ApiTrackingLogger(mock[AccessTokenService], mock[ApiClientService])

    def beLoggable: Matcher[Call] = { c: Call =>
      (t.isLoggable(c.url), s"${c.url} is not loggable")
    }
  }

  "isLoggable" should {

    "return true for all loggables" in new scope {

      val loggables = Seq(
        DraftEditor.load("draftId"),
        DraftDevEditor.load("draftId"),
        ItemEditor.load("itemId"),
        ItemDevEditor.load("itemId"),
        Player.load("sessionId"),
        Player.createSessionForItem("itemId"))
      forall(loggables)(_ must beLoggable)
    }

    "return false for another url" in new scope {
      Call("GET", "some/other/url") must not(beLoggable)
    }
  }

}
