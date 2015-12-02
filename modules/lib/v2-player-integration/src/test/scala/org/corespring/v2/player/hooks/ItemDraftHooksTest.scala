package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.conversion.qti.transformers.{ PlayerJsonToItem, ItemTransformer }
import org.corespring.drafts.item.ItemDrafts
import org.corespring.models.item.Item
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{ OrgCollectionService }
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.Future
import scalaz.{ Success, Validation }

class ItemDraftHooksTest
  extends V2PlayerIntegrationSpec {

  private class scope extends Scope {

    val itemDrafts = mock[ItemDrafts]
    val itemService = mock[ItemService]
    val orgCollectionService = mock[OrgCollectionService]
    val transformer = mock[ItemTransformer]
    val jsonFormatting = mock[JsonFormatting]
    val playerJsonToItem = mock[PlayerJsonToItem]
    def rh = FakeRequest("", "")

    def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = {
      Success(mockOrgAndOpts())
    }

    val hooks = new ItemDraftHooks(
      itemDrafts,
      itemService,
      orgCollectionService,
      transformer,
      jsonFormatting,
      playerJsonToItem,
      getOrgAndOptions,
      containerExecutionContext) {

      override protected def update(draftId: String,
        json: JsValue,
        updateFn: (Item, JsValue) => Item)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
        updateFn(Item(collectionId = ObjectId.get.toString), json)
        Future(Right(json))(ec)
      }

    }
  }

  "ItemDraftHooks" should {

    "load" should {
      "return error of can't find draft and identity" in pending
    }

    "saveProfile" should {

      "save should call update" in new scope {
        val f = hooks.saveProfile("itemId", Json.obj("a" -> "b"))(rh)
        f.map(_.isRight) must equalTo(true).await
      }
    }
  }
}
