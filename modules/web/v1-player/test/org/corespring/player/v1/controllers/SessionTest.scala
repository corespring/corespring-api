
package org.corespring.player.v1.controllers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.{ItemSessionCompanion, DefaultItemSession, PreviewItemSessionCompanion, ItemSession}
import org.corespring.player.accessControl.cookies.PlayerCookieWriter
import org.corespring.player.accessControl.models.RequestedAccess.Mode._
import org.corespring.player.v1.controllers.controllers.{TestBuilder, TestIds}
import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.concurrent.Future

class SessionTest extends Specification with PlayerCookieWriter {


  PlaySingleton.start()

  private def request(m: Mode, content: AnyContent) = {
    implicit val request = FakeRequest("method", "url", FakeHeaders(), content)
    request.withSession(activeModeCookie(m))
  }

  private def itemSession = AnyContentAsJson(Json.toJson(ItemSession(TestIds.testId)))

  import org.specs2.execute.{Result => SpecsResult}

  private def assertAction(msg: String, action: Action[AnyContent], r: FakeRequest[AnyContent], idFn: (Future[SimpleResult] => ObjectId), inPreview: Boolean): SpecsResult = {

    val result = action(r)
    val id = idFn(result)

    def check(companion: ItemSessionCompanion, expectedToBeThere: Boolean) = {
      companion.findOneById(id).map {
        s =>
          if (expectedToBeThere) success else failure(msg + ": we found the item but didn't expect to")
      }.getOrElse(if (expectedToBeThere) failure(msg + ": we couldn't find the item") else success)
    }
    check(PreviewItemSessionCompanion, inPreview)
    check(DefaultItemSession, !inPreview)
  }

  val session = new Session(new TestBuilder)
  "session" should {
    "use the preview collection for preview mode only" in {

      "create" in {
        val action = session.create(TestIds.testId)

        def idFromCreate(r: Future[SimpleResult]): ObjectId = Json.parse(contentAsString(r)).as[ItemSession].id
        assertAction("preview::create", action, request(Preview, itemSession), idFromCreate, inPreview = true)

        forallWhen(List(Render, Administer, Aggregate)) {
          case m => assertAction(m + "::create", action, request(m, itemSession), idFromCreate, inPreview = false).isSuccess === true
        }
      }
    }
  }

}
