package org.corespring.v2player.integration

import org.corespring.it.JsLoadingSpecification
import org.corespring.v2player.integration.actionBuilders.access.V2PlayerCookieKeys

class LoadPlayerJsAsLoggedInUserTest
  extends JsLoadingSpecification {

  "when I'm a logged in user loading the player js" should {
    import org.corespring.container.client.controllers.routes.PlayerLauncher
    "return a cookie" in new callAsUser(PlayerLauncher.playerJs()) {
      status(result) === OK
      session(result).get(V2PlayerCookieKeys.renderOptions).isDefined === true
      session(result).get(V2PlayerCookieKeys.orgId) === Some(orgId.toString)
    }
  }
}
