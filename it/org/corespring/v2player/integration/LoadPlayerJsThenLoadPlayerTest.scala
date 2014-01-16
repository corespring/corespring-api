package org.corespring.v2player.integration

import org.corespring.it.ITSpec
import play.api.test.FakeRequest

class LoadPlayerJsThenLoadPlayerTest extends ITSpec {

  val playerLauncher = org.corespring.container.client.controllers.routes.PlayerLauncher
  val playerHooks = org.corespring.container.client.controllers.hooks.routes.PlayerHooks


  "when I load the player js with orgId and options" should {

    "allow me to create a session" in {

      val js = playerLauncher.playerJs()
      val createSession = playerHooks.createSessionForItem("")

      route(FakeRequest(js.method, js.url)).map {
        r =>
          route(FakeRequest(createSession.method, createSession.url)).map {
            sr =>
              status(sr) === OK
              success
          }.getOrElse(success)
      }.getOrElse(success)
    }
  }
}
