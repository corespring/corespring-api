package org.corespring.v2.player

import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.AddImageAndItem
import org.specs2.matcher.Matcher
import play.api.http.{ ContentTypeOf, Writeable }
import play.api.mvc.{ AnyContent, Request }

class LoadImageIntegrationTest extends IntegrationSpecification {

  def beOk: Matcher[Request[AnyContent]] = { r: Request[AnyContent] =>
    implicit val ct: ContentTypeOf[AnyContent] = new ContentTypeOf[AnyContent](None)
    implicit val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
    val ok = route(r).map { result =>
      status(result) == OK
    }.getOrElse { false }
    (ok, s"result for request returns $OK", s"result for request doesnt return $OK")
  }

  "load image" should {

    "return 200" in new AddImageAndItem {
      override lazy val imagePath = "it/org/corespring/v2/player/load-image/puppy.png"
      import org.corespring.container.client.controllers.apps.routes.Player
      val call = Player.getFile(sessionId.toString, "puppy.png")
      val r = makeRequest(call)
      r must beOk
    }

    import org.corespring.container.client.controllers.apps.routes.Player

    "return 200 when imagePath is encoded" in new AddImageAndItem {
      override lazy val imagePath = "it/org/corespring/v2/player/load-image/pup%20py.png"
      logger.debug(s" in 'return 200 when imagePath is encoded' itemId $itemId")
      val call = Player.getFile(sessionId.toString, "pup%20py.png")
      val r = makeRequest(call)
      r must beOk
    }

    "return 200 when imagePath is not encoded" in new AddImageAndItem {
      override lazy val imagePath = "it/org/corespring/v2/player/load-image/pup py.png"
      logger.debug(s" in 'return 200 when imagePath is encoded' itemId $itemId")
      val call = Player.getFile(sessionId.toString, "pup py.png")
      val r = makeRequest(call)
      r must beOk
    }
  }
}
