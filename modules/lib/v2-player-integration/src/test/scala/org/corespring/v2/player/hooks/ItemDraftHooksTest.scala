package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.conversion.qti.transformers.{ ItemTransformer, PlayerJsonToItem }
import org.corespring.drafts.item.ItemDrafts
import org.corespring.drafts.item.models.{ DraftId, ItemDraft, OrgAndUser }
import org.corespring.models.ContentCollection
import org.corespring.models.item.Item
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrgCollectionService
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.joda.time.DateTime
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.{ Await, Future }
import scalaz.{ Success, Validation }
import scala.concurrent.duration._

class ItemDraftHooksTest
  extends V2PlayerIntegrationSpec with NoTimeConversions {

  private trait scope extends Scope {

    val itemId = ObjectId.get

    val itemDrafts = {
      val m = mock[ItemDrafts]
      m.create(any[DraftId], any[OrgAndUser], any[Option[DateTime]]) answers { (args, _) =>
        val arr = args.asInstanceOf[Array[Any]]
        val draftId = arr(0).asInstanceOf[DraftId]
        val ou = arr(1).asInstanceOf[OrgAndUser]
        Success(ItemDraft(draftId, Item(collectionId = ObjectId.get.toString), ou))
      }
      m
    }

    val itemService = {
      val m = mock[ItemService]
      m.save(any[Item], any[Boolean]) returns Success(VersionedId(itemId, Some(0)))
      m
    }

    val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m.getDefaultCollection(any[ObjectId]) returns Success(ContentCollection("coll", ObjectId.get))
      m
    }

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

  "load" should {
    "return error of can't find draft and identity" in pending
  }

  "saveProfile" should {

    "save should call update" in new scope {
      val f = hooks.saveProfile("itemId", Json.obj("a" -> "b"))(rh)
      f.map(_.isRight) must equalTo(true).await
    }
  }

  "createSingleComponentItemDraft" should {

    trait createSingleComponentItemDraft extends scope {
      lazy val captor = capture[Item]
      val result = Await.result(
        hooks.createSingleComponentItemDraft(None, "component", "key", Json.obj("a" -> "b")),
        1.second)

      there was one(itemService).save(captor, any[Boolean])
    }

    "call item service save with singleComponent xhtml" in new createSingleComponentItemDraft {
      captor.value.playerDefinition.map(_.xhtml) must_== Some("""<div><div component="" id="key"></div></div>""")
    }

    "call item service save with singleComponent json" in new createSingleComponentItemDraft {
      captor.value.playerDefinition.map(_.components) must_== Some(Json.obj("key" -> Json.obj("a" -> "b")))
    }

    "returns the id and name" in new createSingleComponentItemDraft {
      result.right.map(_._1) must_== Right(itemId.toString)
    }
  }

}
