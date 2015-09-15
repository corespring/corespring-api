package org.corespring.api.tracking

import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.{ Mode, Configuration }

class ApiTrackingTest extends Specification with Mockito {

  import org.corespring.container.client.controllers.apps.routes._

  class scope extends Scope {

    val mockToken = mock[AccessTokenService]
    val mockClient = mock[ApiClientService]

    val config = Configuration.from(Map("api.log-requests" -> true))
    val t = new ApiTracking(mockToken, mockClient)(config, Mode.Prod)
  }

  "isLoggable" should {

    "return true for DraftEditor.load" in new scope {
      t.isLoggable(DraftEditor.load("draftId").url) === true
    }

    "return true for DraftDevEditor.load" in new scope {
      t.isLoggable(DraftDevEditor.load("draftId").url) === true
    }

    "return true for ItemEditor.load" in new scope {
      t.isLoggable(ItemEditor.load("itemId").url) === true
    }

    "return true for ItemDevEditor.load" in new scope {
      t.isLoggable(ItemDevEditor.load("itemId").url) === true
    }

    "return true for Player.load" in new scope {
      t.isLoggable(Player.load("sessionId").url) === true
    }

    "return true for Player.createSession" in new scope {
      t.isLoggable(Player.createSessionForItem("itemId").url) === true
    }

    "return false for another url" in new scope {
      t.isLoggable("some/other/url") === false
    }
  }

}
