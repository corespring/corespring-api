package org.corespring.drafts

import com.mongodb.casbah.MongoCollection
import common.db.Db
import org.corespring.drafts.item._
import org.corespring.drafts.item.models.{ ObjectIdAndVersion, SimpleUser }
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.{ ItemServiceWired, ItemService }
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.player.scopes.{ userAndItem, userWithItemAndSession }
import org.specs2.specification.BeforeExample
import play.api.Play

class SimpleDraftTest extends IntegrationSpecification with BeforeExample {

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
      alatestItem.get.taskInfo.get.title === Some("commit a draft")
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

    /*"create a draft, save it, and reload it" in new userAndItem {
      val draft = draftStore.createDraft(itemId.id, SimpleUser(user))

      val newPlayerDef = Some(PlayerDefinition(xhtml = "update!"))
      val update = draft.update(draft.data.copy(playerDefinition = newPlayerDef))
      draftService.save(update)

      draftService.load(update.id).map { d =>
        d.data.playerDefinition.map(_.xhtml) === Some("update!")
      }.getOrElse(failure("didn't find updated draft"))

      draftStore.commitDraft(itemId.id, update) match {
        case Left(errs) => failure("there should be no failures as this is the first committed draft")
        case Right(_) => success
      }
    }*/
  }
}