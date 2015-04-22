package org.corespring.v2.player.hooks

import java.util.concurrent.TimeUnit

import org.corespring.drafts.item.ItemDrafts
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.Duration
import scalaz.{ Success, Validation }

class ItemDraftHooksTest
  extends Specification
  with Mockito
  with MockFactory {

  PlaySingleton.start()

  class __ extends Scope with ItemDraftHooks {

    val mockDrafts = mock[ItemDrafts]
    val mockItemService = mock[ItemService]
    val mockOrgService = mock[OrgService]

    def await[A](f: Future[A]): A = {
      Await.result[A](f, Duration(1, TimeUnit.SECONDS))
    }

    def rh = FakeRequest("", "")

    override def backend: ItemDrafts = mockDrafts

    override def itemService: ItemService = mockItemService

    override def transform: (Item) => JsValue = i => Json.obj()

    override def orgService: OrgService = mockOrgService
    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = {
      Success(mockOrgAndOpts())
    }
  }

  "ItemDraftHooks" should {

    "load" should {
      "return error of can't find draft and identity" in pending
    }

    "saveProfile" should {

      class u extends __ {
        override protected def update(draftId: String,
          json: JsValue,
          updateFn: (Item, JsValue) => Item)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
          updateFn(Item(), json)
          Future(Right(json))
        }
      }

      "save should call update" in new u {
        val result = await[Either[(Int, String), JsValue]] {
          saveProfile("itemId", Json.obj("a" -> "b"))(rh)
        }
        println(result)
        result.isRight must_== true
      }
    }
  }
}
