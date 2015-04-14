package org.corespring.drafts

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import common.db.Db
import org.bson.types.ObjectId
import org.corespring.drafts.errors.{ CommitsAfterDraft, CommitsWithSameSrc }
import org.corespring.drafts.item._
import org.corespring.drafts.item.models.{ SimpleOrg, SimpleUser, OrgAndUser }
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.{ ItemServiceWired, ItemService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.player.scopes.{ userAndItem }
import org.specs2.mock.Mockito
import org.specs2.specification.BeforeExample
import play.api.Play
import play.api.libs.json.Json

import scalaz.{ Success, Failure }

class ItemDraftsTest extends IntegrationSpecification with BeforeExample with Mockito {

  lazy val db = Db.salatDb()(Play.current)

  def bump(vid: VersionedId[ObjectId], count: Int = 1) = vid.copy(version = vid.version.map(_ + count))

  override protected def before: Any = {
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

    lazy val drafts = new ItemDrafts {
      override val itemService: ItemService = ItemServiceWired

      override val draftService: ItemDraftService = ItemDraftsTest.this.draftService

      override val commitService: CommitService = ItemDraftsTest.this.commitService

      val assets = {
        val m = mock[ItemDraftAssets]
        m.copyDraftToItem(any[ObjectId], any[VersionedId[ObjectId]]) answers { (obj, mock) =>
          {
            val arr = obj.asInstanceOf[Array[Any]]
            Success(arr(1).asInstanceOf[VersionedId[ObjectId]])
          }
        }
        m.copyItemToDraft(any[VersionedId[ObjectId]], any[ObjectId]) answers { (obj, mock) =>
          {
            val arr = obj.asInstanceOf[Array[Any]]
            Success(arr(1).asInstanceOf[ObjectId])
          }
        }
        m.deleteDraft(any[ObjectId]) answers { oid => Success(oid.asInstanceOf[ObjectId]) }
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

    "a draft item is unpublished" in new orgAndUserAndItem {
      ItemHelper.update(itemId, Json.obj("$set" -> Json.obj("published" -> true)))
      val draft = drafts.create(itemId, orgAndUser).get
      draft.src.data.published must_== false
    }

    "create a draft of an item" in new orgAndUserAndItem {
      val draft = drafts.create(itemId, orgAndUser)
      draft.flatMap(_.user.user.map(_.id)) === Some(user.id)
    }

    "load a created draft by its id" in new orgAndUserAndItem {
      val draft = drafts.create(itemId, orgAndUser)
      drafts.loadOrCreate(orgAndUser)(draft.get.id).map(_.id) === draft.map(_.id)
    }

    "save a draft" in new orgAndUserAndItem {
      val draft = drafts.create(itemId, orgAndUser).get
      val item = draft.src.data
      val newItem = updateTitle(item, "updated title")
      val update = draft.mkChange(newItem)
      drafts.save(orgAndUser)(update)
      drafts.loadOrCreate(orgAndUser)(draft.id).get.src.data.taskInfo.map(_.title).get === Some("updated title")
    }

    "commit a draft" in new orgAndUserAndItem {
      val draft = drafts.create(itemId, orgAndUser).get
      val item = draft.src.data
      val newItem = updateTitle(item, "commit a draft")
      val update = draft.mkChange(newItem)
      drafts.commit(orgAndUser)(update)
      val latestItem = ItemHelper.get(item.id.copy(version = None))
      latestItem.get.taskInfo.get.title === Some("commit a draft")
      latestItem.get.id.version === Some(0)
    }

    "committing sets the committed DateTime" in new orgAndUserAndItem {
      val draft = drafts.create(itemId, orgAndUser).get
      drafts.commit(orgAndUser)(draft)
      val updatedDraft = drafts.loadOrCreate(orgAndUser)(draft.id).get
      updatedDraft.committed.isDefined === true
      updatedDraft.committed.get.isAfter(draft.created)
    }

    "committing a 2nd draft with the same src id/version fails" in new orgAndUserAndItem {
      val eds = drafts.create(itemId, orgAndUser).get
      val edsSecondDraft = drafts.create(itemId, orgAndUser).get
      val item = eds.src.data
      val newItem = updateTitle(item, "update for committing - 2")
      val update = eds.mkChange(newItem)
      drafts.commit(orgAndUser)(update)
      drafts.commit(orgAndUser)(edsSecondDraft) match {
        case Failure(CommitsAfterDraft(commits)) => {
          val commit = commits(0)
          commit.user === orgAndUser
        }
        case _ => failure("should have returned commits with same src")
      }
    }

    "committing a 2nd draft with the same src id/version and force=true succeeds" in new orgAndUserAndItem {
      val eds = drafts.create(itemId, orgAndUser).get
      val edsSecondDraft = drafts.create(itemId, orgAndUser).get
      val item = eds.src.data
      val newItem = updateTitle(item, "update for committing - 2")
      val update = eds.mkChange(newItem)
      drafts.commit(orgAndUser)(update)
      drafts.commit(orgAndUser)(edsSecondDraft, force = true) match {
        case Failure(CommitsWithSameSrc(commits)) => failure("should have succeeded")
        case _ => success
      }
    }

    "committing a draft to an item that is published" should {

      "add the new version id as the committed id" in new orgAndUserAndItem {
        val eds = drafts.create(itemId, orgAndUser).get
        ItemHelper.update(itemId, Json.obj("$set" -> Json.obj("published" -> true)))
        drafts.commit(orgAndUser)(eds) match {
          case Success(commit) => {
            commit.srcId must_== itemId
            commit.committedId must_== bump(itemId)
          }
          case _ => failure("should have been successful committing")
        }

      }

      "update the src id to the latest id" in new orgAndUserAndItem {
        val eds = drafts.create(itemId, orgAndUser).get
        ItemHelper.update(itemId, Json.obj("$set" -> Json.obj("published" -> true)))
        drafts.commit(orgAndUser)(eds)
        val dbDraft = drafts.loadOrCreate(orgAndUser)(eds.id).get
        dbDraft.src.id must_== bump(itemId)
      }
    }

    "in a typical workflow" should {

      "after committing to a published item, later commits will target the subsequent version" in new orgAndUserAndItem {
        val eds = drafts.create(itemId, orgAndUser).get
        ItemHelper.update(itemId, Json.obj("$set" -> Json.obj("published" -> true)))
        drafts.commit(orgAndUser)(eds)
        val newDraft = drafts.loadOrCreate(orgAndUser)(eds.id).get

        newDraft.src.id must_== bump(itemId)

        drafts.commit(orgAndUser)(newDraft) match {
          case Success(commit) => {

            commit.srcId must_== newDraft.src.id
            commit.committedId must_== newDraft.src.id
          }
          case _ => failure("should have been a successful commit")
        }

      }
    }

    "publishing a draft" should {
      "set published to true, commit and delete the draft" in new orgAndUserAndItem {
        val eds = drafts.create(itemId, orgAndUser).get
        val update = eds.mkChange(eds.src.data.copy(taskInfo = eds.src.data.taskInfo.map(_.copy(title = Some("update")))))

        update.src.data.published must_== false

        val saveResult = drafts.save(orgAndUser)(update)
        val publishedItemId = drafts.publish(orgAndUser)(eds.id)
        drafts.collection.findOneByID(eds.id) must_== None
        val commits = commitService.findByIdAndVersion(itemId.id, itemId.version.get)
        commits.length must_== 1
        commits(0).draftId must_== eds.id

        val item = ItemHelper.get(commits(0).committedId).get

        item.taskInfo.flatMap(_.title) must_== Some("update")
        item.published must_== true
      }
    }

    "publishing multiple times keeps bumping the version of the item" in new orgAndUserAndItem {

      val draftOne = drafts.create(itemId, orgAndUser).get
      val update = draftOne.mkChange(draftOne.src.data.copy(taskInfo = draftOne.src.data.taskInfo.map(_.copy(title = Some("update")))))
      drafts.save(orgAndUser)(update)
      drafts.publish(orgAndUser)(draftOne.id)

      val two = drafts.create(itemId, orgAndUser).get

      two.src.data.id must_== bump(itemId)

      val updateTwo = two.mkChange(two.src.data.copy(taskInfo = two.src.data.taskInfo.map(_.copy(title = Some("update")))))
      drafts.save(orgAndUser)(updateTwo)

      two.id must_== updateTwo.id

      drafts.publish(orgAndUser)(two.id) match {
        case Success(vid) => {
          vid must_== bump(itemId, 1)
          there was one(drafts.assets).copyDraftToItem(draftOne.id, itemId) andThen one(drafts.assets).copyDraftToItem(two.id, vid)
        }
        case _ => failure("publish should have been successful")
      }
    }
  }
}