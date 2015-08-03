package org.corespring.wiring.apiTracking

import org.corespring.models.auth.ApiClientService
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.services.TokenService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ApiTrackingTest extends Specification with Mockito {

  PlaySingleton.start()

  import org.corespring.container.client.controllers.apps.routes._

  class scope extends Scope {

    val mockToken = mock[TokenService]
    val mockClient = mock[ApiClientService]

    val t = new ApiTracking(mockToken, mockClient)
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
