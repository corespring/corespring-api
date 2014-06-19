package org.corespring.v2player.integration

import org.corespring.it.JsLoadingSpecification
import org.corespring.v2player.integration.cookies.V2PlayerCookieKeys

class LoadEditorJsAsLoggedInUserTest
  extends JsLoadingSpecification {
  "when I'm a logged in user, loading the editor js" should {
    import org.corespring.container.client.controllers.routes.PlayerLauncher
    "load with a session cookie" in new callAsUser(PlayerLauncher.editorJs()) {
      status(result) === OK
      session(result).get(V2PlayerCookieKeys.renderOptions).isDefined === true
      session(result).get(V2PlayerCookieKeys.orgId) === Some(orgId.toString)
    }
  }
}
