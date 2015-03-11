package org.corespring.drafts

import com.mongodb.casbah.MongoCollection
import common.db.Db
import org.bson.types.ObjectId
import org.corespring.drafts.item._
import org.corespring.drafts.item.models.SimpleUser
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.PlayerDefinition
import org.corespring.platform.core.services.item.{ ItemServiceWired, ItemService }
import org.corespring.v2.player.scopes.{ userAndItem, userWithItemAndSession }
import play.api.Play

class SimpleDraftTest extends IntegrationSpecification {

  lazy val draftStore = new ItemDraftStore {
    override def itemService: ItemService = ItemServiceWired

    override def commitService: CommitService = new ItemCommitService {
      override def collection: MongoCollection = Db.salatDb()(Play.current)("drafts.item.commits")

    }
  }

  lazy val draftService = new ItemDraftService {
    override def collection: MongoCollection = Db.salatDb()(Play.current)("drafts.item")
  }

  "Single Draft Api" should {

    trait SimpleApi {
      def createDraft(id: ObjectId, user: SimpleUser): ItemUserDraft
    }

    val api = new SimpleApi {
      override def createDraft(id: ObjectId, user: SimpleUser): ItemUserDraft = ???
    }

    "be simple" in new userAndItem {
      //creates the draft and saves it
      val draft = api.createDraft(itemId.id, SimpleUser(user))

      // returns a draft
      // api.loadDraft(draft.id)
      // returns all drafts for user and itemId
      // api.loadDraftsByUserAndItem(itemId.id, SimpleUser(user)) : Seq[ItemUserDraft]
      // returns all drafts for the item
      // api.loadDraftsByItemId(itemId.id) : Seq[ItemUserDraft]
      // commit a draft
      // api.commitDraft(draft, force:Boolean) : Either[E,CommitInfo]
    }

  }
  "SimpleDraftTest" should {

    "create a draft of an item" in new userAndItem {
      val draft = draftStore.createDraft(itemId.id, SimpleUser(user))
      draft.user.id === user.id
    }

    "create a draft, save it, and reload it" in new userAndItem {
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
    }
  }
}