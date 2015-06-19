package org.corespring.v2.player

import org.corespring.it.IntegrationSpecification
import org.corespring.v2.player.scopes.AddImageAndItem

class LoadImageIntegrationTest extends IntegrationSpecification {

  "load image" should {

    "return 200" in new AddImageAndItem("it/org/corespring/v2/player/load-image/puppy.png") {

      import org.corespring.container.client.controllers.apps.routes.Player

      logger.debug(s" in 'return 200' itemId $itemId")
      val call = Player.getFile(sessionId.toString, "puppy.png")
      val r = makeRequest(call)
      route(r)(writeable).map { r =>
        status(r) === OK
      }.getOrElse {
        failure("Can't load asset")
      }
    }

    import org.corespring.container.client.controllers.apps.routes.Player

    "return 200 when imagePath is encoded" in new AddImageAndItem("it/org/corespring/v2/player/load-image/pup%20py.png") {
      logger.debug(s" in 'return 200 when imagePath is encoded' itemId $itemId")
      val call = Player.getFile(sessionId.toString, "pup%20py.png")
      val r = makeRequest(call)
      route(r)(writeable).map { r =>
        status(r) === OK
      }.getOrElse(failure("Can't load asset"))
    }

    "return 200 when imagePath is not encoded" in new AddImageAndItem("it/org/corespring/v2/player/load-image/pup py.png") {
      logger.debug(s" in 'return 200 when imagePath is encoded' itemId $itemId")
      val call = Player.getFile(sessionId.toString, "pup py.png")
      val r = makeRequest(call)
      route(r)(writeable).map { r =>
        status(r) === OK
      }.getOrElse(failure("Can't load asset"))
    }
  }
}
