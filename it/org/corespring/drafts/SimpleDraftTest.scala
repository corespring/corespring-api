package org.corespring.drafts

import com.amazonaws.services.s3.AmazonS3Client
import com.mongodb.casbah.MongoCollection
import common.db.Db
import org.bson.types.ObjectId
import org.corespring.drafts.errors.CommitsWithSameSrc
import org.corespring.drafts.item._
import org.corespring.drafts.item.models.{ OrgAndUser, ObjectIdAndVersion, SimpleUser }
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

import scalaz.{ Success, Failure }

class SimpleDraftTest extends IntegrationSpecification with BeforeExample with Mockito {

  lazy val db = Db.salatDb()(Play.current)

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

  lazy val drafts = new ItemDrafts {
    override def itemService: ItemService = ItemServiceWired

    override def draftService: ItemDraftService = SimpleDraftTest.this.draftService

    override def commitService: CommitService = SimpleDraftTest.this.commitService

    override def assets: ItemDraftAssets = {
      val m = mock[ItemDraftAssets]
      m.copyDraftToItem(any[ObjectId], any[VersionedId[ObjectId]]) answers { (oid, vid) => Success(vid.asInstanceOf[VersionedId[ObjectId]]) }
      m.copyItemToDraft(any[VersionedId[ObjectId]], any[ObjectId]) answers { (vid, oid) => Success(oid.asInstanceOf[ObjectId]) }
      m.deleteDraft(any[ObjectId]) answers { oid => Success(oid.asInstanceOf[ObjectId]) }
      m
    }

    /** Check that the user may create the draft for the given src id */
    override protected def userCanCreateDraft(id: ObjectId, user: OrgAndUser): Boolean = true
  }

  def updateTitle(item: Item, title: String): Item = {
    item.copy(taskInfo = item.taskInfo.map(_.copy(title = Some(title))))
  }

  "SimpleDraftTest" should {

    "create a draft of an item" in new userAndItem {
      val draft = drafts.create(itemId.id, SimpleUser(user))
      draft.map(_.user.id) === Some(user.id)
    }

    "load a created draft by its id" in new userAndItem {
      val draft = drafts.create(itemId.id, SimpleUser(user))
      drafts.load(draft.get.id) === draft
    }

    "save a draft" in new userAndItem {
      val draft = drafts.create(itemId.id, SimpleUser(user)).get
      val item = draft.src.data
      val newItem = updateTitle(item, "updated title")
      val update = draft.update(newItem)
      drafts.save(update)
      drafts.load(draft.id).get.src.data.taskInfo.map(_.title).get === Some("updated title")
    }

    "commit a draft" in new userAndItem {
      val draft = drafts.create(itemId.id, SimpleUser(user)).get
      val item = draft.src.data
      val newItem = updateTitle(item, "commit a draft")
      val update = draft.update(newItem)
      drafts.commit(update)
      val latestItem = ItemHelper.get(item.id.copy(version = None))
      latestItem.get.taskInfo.get.title === Some("commit a draft")
      latestItem.get.id.version === Some(1)
    }

    "committing a draft, removes the draft and creates a commit" in new userAndItem {
      val draft = drafts.create(itemId.id, SimpleUser(user)).get
      val item = draft.src.data
      val newItem = updateTitle(item, "update for committing - 2")
      val update = draft.update(newItem)
      drafts.commit(update)
      drafts.load(update.id) === None
      val commits = drafts.loadCommits(ObjectIdAndVersion(itemId.id, itemId.version.get))
      commits.length === 1
      val commit = commits(0)
      commit.user.userName === user.userName
    }

    "committing a 2nd draft with the same src id/version fails" in new userAndItem {
      val eds = drafts.create(itemId.id, SimpleUser(user)).get
      val edsSecondDraft = drafts.create(itemId.id, SimpleUser(user)).get
      val item = eds.src.data
      val newItem = updateTitle(item, "update for committing - 2")
      val update = eds.update(newItem)
      drafts.commit(update)
      drafts.commit(edsSecondDraft) match {
        case Failure(CommitsWithSameSrc(commits)) => {
          val commit = commits(0)
          commit.user === SimpleUser(user)
        }
        case _ => failure("should have returnd commits with same src")
      }
    }

    "committing a 2nd draft with the same src id/version and force=true succeeds" in new userAndItem {
      val eds = drafts.create(itemId.id, SimpleUser(user)).get
      val edsSecondDraft = drafts.create(itemId.id, SimpleUser(user)).get
      val item = eds.src.data
      val newItem = updateTitle(item, "update for committing - 2")
      val update = eds.update(newItem)
      drafts.commit(update)
      drafts.commit(edsSecondDraft, force = true) match {
        case Failure(CommitsWithSameSrc(commits)) => failure("should have succeeded")
        case _ => success
      }
    }

  }
}