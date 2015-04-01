package org.corespring.drafts

import com.mongodb.casbah.MongoCollection
import common.db.Db
import org.bson.types.ObjectId
import org.corespring.drafts.errors.CommitsWithSameSrc
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

class SimpleDraftTest extends IntegrationSpecification with BeforeExample with Mockito {

  lazy val db = Db.salatDb()(Play.current)

  def bump(vid: VersionedId[ObjectId]) = vid.copy(version = vid.version.map(_ + 1))

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

  trait orgAndUserAndItem extends userAndItem {
    lazy val orgAndUser: OrgAndUser = OrgAndUser(SimpleOrg(user.org.orgId, "?"), Some(SimpleUser.fromUser(user)))
  }

  def updateTitle(item: Item, title: String): Item = {
    item.copy(taskInfo = item.taskInfo.map(_.copy(title = Some(title))))
  }

  "SimpleDraftTest" should {

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
      drafts.load(orgAndUser)(draft.get.id).map(_.id) === draft.map(_.id)
    }

    "save a draft" in new orgAndUserAndItem {
      val draft = drafts.create(itemId, orgAndUser).get
      val item = draft.src.data
      val newItem = updateTitle(item, "updated title")
      val update = draft.update(newItem)
      drafts.save(orgAndUser)(update)
      drafts.load(orgAndUser)(draft.id).get.src.data.taskInfo.map(_.title).get === Some("updated title")
    }

    "commit a draft" in new orgAndUserAndItem {
      val draft = drafts.create(itemId, orgAndUser).get
      val item = draft.src.data
      val newItem = updateTitle(item, "commit a draft")
      val update = draft.update(newItem)
      drafts.commit(orgAndUser)(update)
      val latestItem = ItemHelper.get(item.id.copy(version = None))
      latestItem.get.taskInfo.get.title === Some("commit a draft")
      latestItem.get.id.version === Some(0)
    }

    "committing a draft, keeps the draft and creates a commit" in new orgAndUserAndItem {
      val draft = drafts.create(itemId, orgAndUser).get
      val item = draft.src.data
      val newItem = updateTitle(item, "update for committing - 2")
      val update = draft.update(newItem)
      drafts.commit(orgAndUser)(update)
      drafts.load(orgAndUser)(update.id) must_!= None
      println(s"load commits for: $itemId")
      val commits = drafts.loadCommits(itemId)
      commits.length === 1
      val commit = commits(0)
      commit.user.user.map(_.userName) === Some(user.userName)
    }

    "committing a 2nd draft with the same src id/version fails" in new orgAndUserAndItem {
      val eds = drafts.create(itemId, orgAndUser).get
      val edsSecondDraft = drafts.create(itemId, orgAndUser).get
      val item = eds.src.data
      val newItem = updateTitle(item, "update for committing - 2")
      val update = eds.update(newItem)
      drafts.commit(orgAndUser)(update)
      drafts.commit(orgAndUser)(edsSecondDraft) match {
        case Failure(CommitsWithSameSrc(commits)) => {
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
      val update = eds.update(newItem)
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
        val dbDraft = drafts.load(orgAndUser)(eds.id).get
        dbDraft.src.id must_== bump(itemId)
      }
    }

    "in a typical workflow" should {

      "after committing to a published item, later commits will target the subsequent version" in new orgAndUserAndItem {
        val eds = drafts.create(itemId, orgAndUser).get
        ItemHelper.update(itemId, Json.obj("$set" -> Json.obj("published" -> true)))
        drafts.commit(orgAndUser)(eds)
        val newDraft = drafts.load(orgAndUser)(eds.id).get

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
  }
}