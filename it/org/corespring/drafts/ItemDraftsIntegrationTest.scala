package org.corespring.drafts

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.drafts.errors.NothingToCommit
import org.corespring.drafts.item._
import org.corespring.drafts.item.models._
import org.corespring.it.IntegrationSpecification
import org.corespring.it.assets.{ ImageUtils, PlayerDefinitionImageUploader }
import org.corespring.it.scopes.userAndItem
import org.corespring.models.item.resource.StoredFile
import org.corespring.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.BeforeAfter

import scalaz.{ Failure, Success }

class ItemDraftsIntegrationTest extends IntegrationSpecification {

  lazy val itemService = main.itemService
  lazy val draftService = main.draftService

  def bump(vid: VersionedId[ObjectId], count: Int = 1) = vid.copy(version = vid.version.map(_ + count))

  trait orgAndUserAndItem extends userAndItem with BeforeAfter {
    lazy val orgAndUser: OrgAndUser = OrgAndUser(SimpleOrg(user.org.orgId, "?"), Some(SimpleUser.fromUser(user)))

    lazy val item = itemService.findOneById(itemId).get

    lazy val drafts = main.itemDrafts

    override def before: Any = {
      super.before
      println(s"[userAndItem] dropping the collection")
      draftService.collection.dropCollection()
    }

    override def after: Any = {
      super.after
      draftService.collection.dropCollection()
    }
  }

  def updateTitle(item: Item, title: String): Item = {
    item.copy(taskInfo = item.taskInfo.map(_.copy(title = Some(title))))
  }

  def draftIdFromItemIdAndUser(itemId: VersionedId[ObjectId], o: OrgAndUser): DraftId = {
    DraftId(itemId.id, o.user.map(_.userName).getOrElse("test"), o.org.id)
  }

  "ItemDrafts" should {

    "load/create" should {
      "create a new draft for a user" in new orgAndUserAndItem {

        draftService.collection.count(MongoDBObject()) must_== 0

        drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)) match {
          case Success(draft) => {
            draft.parent.data must_== item
            draftService.collection.count(MongoDBObject()) must_== 1
          }
          case _ => failure("should have been successful")
        }
      }

      "load a user's existing draft" in new orgAndUserAndItem {
        draftService.collection.count(MongoDBObject()) must_== 0
        drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser))
        draftService.collection.count(MongoDBObject()) must_== 1
        drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser))
        draftService.collection.count(MongoDBObject()) must_== 1
      }

      "load a user's existing draft with their changes if they have non committed changes" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val update = draft.mkChange(draft.change.data.copy(playerDefinition = Some(PlayerDefinition("Change"))))
        drafts.save(orgAndUser)(update)
        itemService.save(item.copy(playerDefinition = Some(PlayerDefinition("hello!"))))
        drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser), true) match {
          case Success(d) => {
            d.parent.data.playerDefinition must_== None
            d.change.data.playerDefinition.map(_.xhtml) must_== Some("Change")
          }
          case _ => failure("should have been successful")
        }
      }

      "loads the latest item if the user doesn't have any changes in their draft" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val update = draft.mkChange(draft.change.data.copy(playerDefinition = Some(PlayerDefinition("change 1"))))
        drafts.commit(orgAndUser)(update)

        itemService.findOneById(itemId).map { i =>
          val itemUpdate = i.copy(playerDefinition = Some(PlayerDefinition("change 1 - from another draft")))
          itemService.save(itemUpdate)
        }

        drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)) match {
          case Success(d) => {
            d.parent.data.playerDefinition.map(_.xhtml) must_== Some("change 1 - from another draft")
            d.change.data.playerDefinition.map(_.xhtml) must_== Some("change 1 - from another draft")
          }
          case Failure(e) => failure("load should have been successful")
        }

      }

      "return a draft is out of date error, if the draft has non committed changes and the draft.parent != item" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val update = draft.mkChange(draft.change.data.copy(playerDefinition = Some(PlayerDefinition("Change"))))
        drafts.save(orgAndUser)(update)

        itemService.save(item.copy(playerDefinition = Some(PlayerDefinition("hello!"))))

        val updatedItem = itemService.findOneById(item.id).get

        drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)) match {
          case Success(d) => failure("should have failed")
          case Failure(e) => {
            e.msg must_== ItemDraftIsOutOfDate(update, ItemSrc(updatedItem)).msg
          }
        }
      }
    }

    "commit" should {

      "return an error if there is nothing to commit" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val commit = drafts.commit(orgAndUser)(draft) must_== Failure(NothingToCommit(draft.id))
      }

      "allow a commit if the parent matches the item" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val update = draft.mkChange(item.copy(playerDefinition = Some(PlayerDefinition("change"))))
        val commit = drafts.commit(orgAndUser)(update).toOption.get
        commit.draftId must_== draft.id
        commit.srcId must_== item.id
        commit.user must_== orgAndUser
      }

      "return an ItemDraftIsOutOfDate error commit if the parent does not match the item" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val update = draft.mkChange(item.copy(playerDefinition = Some(PlayerDefinition("change"))))
        itemService.save(item.copy(playerDefinition = Some(PlayerDefinition("change"))))
        drafts.commit(orgAndUser)(update) must_== Failure(ItemDraftIsOutOfDate(update, ItemSrc(itemService.findOneById(itemId).get)))
      }

      "allow a forced commit of a conflicted item" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val update = draft.mkChange(draft.change.data.copy(playerDefinition = Some(PlayerDefinition("!!"))))
        itemService.save(item.copy(playerDefinition = Some(PlayerDefinition("change"))))
        drafts.commit(orgAndUser)(update) must_== Failure(ItemDraftIsOutOfDate(update, ItemSrc(itemService.findOneById(itemId).get)))
        drafts.commit(orgAndUser)(update, true) match {
          case Success(commit) => success
          case _ => failure("should have been successful")
        }
        update.change.data.playerDefinition.map(_.xhtml) must_== Some("!!")
        itemService.findOneById(itemId).get.playerDefinition.map(_.xhtml) must_== Some("!!")
      }

      "updates the draft parent so subsequent commits will work" in new orgAndUserAndItem {
        val draftOne = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val updateOne = draftOne.mkChange(draftOne.change.data.copy(playerDefinition = Some(PlayerDefinition("1"))))
        drafts.commit(orgAndUser)(updateOne).isSuccess must_== true
        val draftTwo = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        draftTwo.parent.data.playerDefinition must_== Some(PlayerDefinition("1"))
      }

      "allows multiple commits" in new orgAndUserAndItem {
        val draftOne = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val updateOne = draftOne.mkChange(draftOne.change.data.copy(playerDefinition = Some(PlayerDefinition("1"))))
        drafts.commit(orgAndUser)(updateOne).isSuccess must_== true

        val draftTwo = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        draftTwo.parent.data.playerDefinition must_== Some(PlayerDefinition("1"))
        val updateTwo = draftTwo.mkChange(draftTwo.change.data.copy(playerDefinition = Some(PlayerDefinition("2"))))
        updateTwo.parent.data.playerDefinition must_== Some(PlayerDefinition("1"))
        drafts.commit(orgAndUser)(updateTwo).isSuccess must_== true

        val draftThree = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        draftThree.parent.data.playerDefinition must_== Some(PlayerDefinition("2"))
        val updateThree = draftThree.mkChange(draftThree.change.data.copy(playerDefinition = Some(PlayerDefinition("3"))))
        updateThree.parent.data.playerDefinition must_== Some(PlayerDefinition("2"))
        drafts.commit(orgAndUser)(updateThree).isSuccess must_== true
      }
    }

    "conflict" should {
      "return no conflict" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        drafts.conflict(orgAndUser)(draft.id) must_== Success(None)
      }

      "return a conflict" in new orgAndUserAndItem {
        val draft = drafts.loadOrCreate(orgAndUser)(draftIdFromItemIdAndUser(itemId, orgAndUser)).toOption.get
        val updatedItem = item.copy(playerDefinition = Some(PlayerDefinition("change")))
        itemService.save(updatedItem)
        drafts.conflict(orgAndUser)(draft.id) match {
          case Success(Some(c)) => success
          case _ => failure("should have been successful")
        }
      }
    }

    "clone" should {
      "copy the assets over to the new draft" in new orgAndUserAndItem {
        PlayerDefinitionImageUploader.uploadImageAndAddToPlayerDefinition(
          itemId,
          "/test-images/puppy.png")

        val draftId = draftIdFromItemIdAndUser(itemId, orgAndUser)
        val draft = drafts.loadOrCreate(orgAndUser)(draftId)
        val expectedPath = S3Paths.draftFile(draftId, "puppy.png")
        println(s"expectedPath: $expectedPath")
        ImageUtils.exists(expectedPath) must_== true
        drafts.cloneDraft(orgAndUser)(draftId) match {
          case Success(r) => {
            ImageUtils.exists(S3Paths.draftFile(r.draftId, "puppy.png")) must_== true
          }
          case Failure(e) => failure("clone should have been successful")
        }
      }
    }

    "addFileToChangeSet" should {
      "add the file info to the ItemDraft.change" in new orgAndUserAndItem {
        val draftId = draftIdFromItemIdAndUser(itemId, orgAndUser)
        val draft = drafts.loadOrCreate(orgAndUser)(draftId)
        val file = StoredFile("test.png", "image/png", false)
        drafts.addFileToChangeSet(draft.toOption.get, file)
        draftService.load(draftId).map { dbDraft =>
          dbDraft.change.data.playerDefinition.map(_.files.find(_.name == "test.png").headOption).flatten must_== Some(file)
        }.getOrElse(failure("should have loaded the draft"))
      }
    }

    "listForOrg" should {

      "list drafts" in new orgAndUserAndItem {
        val draftId = draftIdFromItemIdAndUser(itemId, orgAndUser)
        drafts.loadOrCreate(orgAndUser)(draftId)
        val secondDraftId = DraftId(itemId.id, "other-draft", orgAndUser.org.id)
        drafts.loadOrCreate(orgAndUser.copy(user = None))(secondDraftId)
        println(s"orgId: ${orgAndUser.org.id}")
        drafts.listForOrg(orgAndUser.org.id).length === 2
      }
    }
  }

}