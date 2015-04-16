package org.corespring.drafts

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import common.db.Db
import org.bson.types.ObjectId
import org.corespring.drafts.errors.DraftIsOutOfDate
import org.corespring.drafts.item._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ CommitService, ItemDraftService }
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.{ PlayerDefinition, Item }
import org.corespring.platform.core.services.item.{ ItemPublishingService, ItemService, ItemServiceWired }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.player.scopes.userAndItem
import org.specs2.mock.Mockito
import org.specs2.specification.BeforeExample
import play.api.Play

import scalaz.{ Failure, Success }

class ItemDraftsTest extends IntegrationSpecification with BeforeExample with Mockito {

  lazy val db = Db.salatDb()(Play.current)

  def bump(vid: VersionedId[ObjectId], count: Int = 1) = vid.copy(version = vid.version.map(_ + count))

  override protected def before: Any = {
    println(s"--> dropping it.drafts.item and it.drafts.item_commits")

    db("it.drafts.item").drop()
    db("it.drafts.item_commits").drop()
  }

  lazy val draftService = new ItemDraftService {
    override def collection: MongoCollection = db("it.drafts.item")
  }

  lazy val commitService = new CommitService {
    override def collection: MongoCollection = db("it.drafts.item_commits")
  }

  trait orgAndUserAndItem extends userAndItem {
    lazy val orgAndUser: OrgAndUser = OrgAndUser(SimpleOrg(user.org.orgId, "?"), Some(SimpleUser.fromUser(user)))

    lazy val item = ItemServiceWired.findOneById(itemId).get

    lazy val drafts = new ItemDrafts {
      override val itemService: ItemService with ItemPublishingService = ItemServiceWired

      override val draftService: ItemDraftService = ItemDraftsTest.this.draftService

      override val commitService: CommitService = ItemDraftsTest.this.commitService

      val assets = {
        val m = mock[ItemDraftAssets]
        m.copyDraftToItem(any[DraftId], any[VersionedId[ObjectId]]) answers { (obj, mock) =>
          {
            val arr = obj.asInstanceOf[Array[Any]]
            Success(arr(1).asInstanceOf[VersionedId[ObjectId]])
          }
        }
        m.copyItemToDraft(any[VersionedId[ObjectId]], any[DraftId]) answers { (obj, mock) =>
          {
            val arr = obj.asInstanceOf[Array[Any]]
            Success(arr(1).asInstanceOf[DraftId])
          }
        }
        m.deleteDraft(any[DraftId]) answers { oid => Success(oid.asInstanceOf[ObjectId]) }
        m
      }

      /** Check that the user may create the draft for the given src id */
      override protected def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean = true

    }
  }

  def updateTitle(item: Item, title: String): Item = {
    item.copy(taskInfo = item.taskInfo.map(_.copy(title = Some(title))))
  }

  "ItemDrafts" should {

    "load/create" should {
      "create a new draft for a user" in new orgAndUserAndItem {

        draftService.collection.count(MongoDBObject()) must_== 0

        drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser)) match {
          case Success(draft) => {
            draft.parent.data must_== item
            draftService.collection.count(MongoDBObject()) must_== 1
          }
          case _ => failure("should have been successful")
        }
      }

      "load a user's existing draft" in new orgAndUserAndItem {
        draftService.collection.count(MongoDBObject()) must_== 0
        drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser))
        draftService.collection.count(MongoDBObject()) must_== 1
        drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser))
        draftService.collection.count(MongoDBObject()) must_== 1
      }

      "load a user's existing draft with the latest update of item" in new orgAndUserAndItem {
        drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser))
        ItemServiceWired.save(item.copy(playerDefinition = Some(PlayerDefinition("hello!"))))
        drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser)) match {
          case Success(d) => d.parent.data.playerDefinition must_== Some(PlayerDefinition("hello!"))
          case _ => failure("should have been successful")
        }
      }

      "load a user's existing draft with no update because it's conflicted" in new orgAndUserAndItem {
        drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser)) match {
          case Success(d) => {
            val update = d.copy(hasConflict = true)
            draftService.save(update)
          }
          case _ => failure("should have been succesful")
        }

        ItemServiceWired.save(item.copy(playerDefinition = Some(PlayerDefinition("hello!"))))

        drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser)) match {
          case Success(d) => d.parent.data.playerDefinition must_== None
          case _ => failure("should have been successful")
        }
      }
    }

    "commit" should {
      "allow a commit if the parent matches the item" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser)).toOption.get
        val commit = drafts.commit(orgAndUser)(draft).toOption.get
        commit.draftId must_== draft.id
        commit.srcId must_== item.id
        commit.user must_== orgAndUser
      }

      "not allow a commit if the parent does not match the item" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser)).toOption.get
        ItemServiceWired.save(item.copy(playerDefinition = Some(PlayerDefinition("change"))))
        drafts.commit(orgAndUser)(draft) must_== Failure(DraftIsOutOfDate(draft.copy(hasConflict = true), ItemSrc(ItemServiceWired.findOneById(itemId).get)))
      }

      "flags the draft as conflicted if the parent does not match the item" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(DraftId.fromIdAndUser(itemId, orgAndUser)).toOption.get
        ItemServiceWired.save(item.copy(playerDefinition = Some(PlayerDefinition("change"))))
        drafts.commit(orgAndUser)(draft) must_== Failure(DraftIsOutOfDate(draft.copy(hasConflict = true), ItemSrc(ItemServiceWired.findOneById(itemId).get)))
        drafts.load(orgAndUser)(draft.id).toOption.get.hasConflict must_== true
        //drafts.commit(orgAndUser)(draft) must_== Failure(DraftIsOutOfDate(draft.copy(hasConflict = true), ItemSrc(ItemServiceWired.findOneById(itemId).get)))
      }

      "allow a forced commit of a conflicted item" in pending
    }

    "show what's changed" should {
      "show what's changed" in pending
    }

  }
}