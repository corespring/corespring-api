package org.corespring.drafts.item

import com.mongodb.{ CommandResult, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.errors.{ UserCantCommit, DeleteFailed, UserCantSave }
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Success, Failure }

class ItemDraftsTest extends Specification with Mockito {

  private trait TestDrafts extends ItemDrafts {
    override def itemService: ItemService = ???

    override def draftService: ItemDraftService = ???

    override def assets: ItemDraftAssets = ???

    override def commitService: CommitService = ???

    /** Check that the user may create the draft for the given src id */
    override protected def userCanCreateDraft(id: ObjectId, user: OrgAndUser): Boolean = ???
  }

  "ItemDrafts" should {

    val ed = OrgAndUser(SimpleOrg(ObjectId.get, "ed-org"), None)
    val gwen = OrgAndUser(SimpleOrg(ObjectId.get, "gwen-org"), None)
    val item = Item(id = VersionedId(ObjectId.get, Some(0)))

    "when creating" should {

      class scp(canCreate: Boolean) extends Scope {
        val gwensDraft = ItemDraft(ObjectId.get, ItemSrc(item, ObjectIdAndVersion(item.id.id, 0)), gwen)
        lazy val drafts = new TestDrafts {

          override def userCanCreateDraft(id: ObjectId, user: OrgAndUser) = canCreate

          override val itemService: ItemService = {
            mock[ItemService].findOneById(any[VersionedId[ObjectId]]) returns Some(item)
          }

          override val assets: ItemDraftAssets = mock[ItemDraftAssets].copyItemToDraft(any[VersionedId[ObjectId]], any[ObjectId]) answers { (obj, mock) =>
            val arr = obj.asInstanceOf[Array[Any]]
            Success(arr(1).asInstanceOf[ObjectId])
          }

          override val draftService: ItemDraftService = {
            val m = mock[ItemDraftService]
            m.save(any[ItemDraft]) returns {
              mock[WriteResult].getLastError.returns(mock[CommandResult].ok returns true)
            }
            m
          }
        }
      }

      "return error if create" in new scp(false) {
        drafts.create(ObjectId.get, ed) must_== None
      }

      "return error if create" in new scp(true) {
        drafts.create(ObjectId.get, ed) match {
          case Some(ItemDraft(_, _, _, _, _)) => success
          case _ => failure("should have got an item draft")
        }
      }
    }

    "when saving" should {

      class scp extends Scope {
        val gwensDraft = ItemDraft(ObjectId.get, ItemSrc(item, ObjectIdAndVersion(item.id.id, 0)), gwen)
        lazy val drafts = new TestDrafts {
          override def draftService: ItemDraftService = {
            val m = mock[ItemDraftService]
            m.save(any[ItemDraft]) returns {
              mock[WriteResult].getLastError.returns(mock[CommandResult].ok returns true)
            }
            m
          }
        }
      }

      "not allow ed to save gwen's draft" in new scp {
        drafts.save(ed)(gwensDraft) must_== Failure(UserCantSave(ed, gwen))
      }

      "allow gwen to save gwen's draft" in new scp {
        drafts.save(gwen)(gwensDraft) must_== Success(gwensDraft.id)
      }
    }

    "when removing a draft" should {

      class scp(n: Int) extends Scope {
        val oid = ObjectId.get
        lazy val drafts = new TestDrafts {
          override val draftService: ItemDraftService = mock[ItemDraftService]
            .removeDraftByIdAndUser(any[ObjectId], any[OrgAndUser])
            .returns(mock[WriteResult].getN returns n)

          override val assets: ItemDraftAssets = mock[ItemDraftAssets].deleteDraft(any[ObjectId]) returns Success(Unit)
        }
      }

      "return delete failed" in new scp(0) {
        drafts.removeDraftByIdAndUser(oid, ed) must_== Failure(DeleteFailed)
      }

      "return draft id" in new scp(1) {
        drafts.removeDraftByIdAndUser(oid, ed) must_== Success(oid)
        there was one(drafts.assets).deleteDraft(oid)
      }
    }

    "when committing" should {

      class scp extends Scope {
        val gwensDraft = ItemDraft(ObjectId.get, ItemSrc(item, ObjectIdAndVersion(item.id.id, 0)), gwen)
        lazy val drafts = new TestDrafts {
          override val draftService: ItemDraftService = {
            val m = mock[ItemDraftService]
            m.listForOrg(any[ObjectId]) returns Seq.empty
            m.save(any[ItemDraft]) returns {
              mock[WriteResult].getLastError.returns(mock[CommandResult].ok returns true)
            }

            m.remove(any[ItemDraft]) returns true
            m
          }

          override val commitService: CommitService = {
            val m = mock[CommitService]
            m.save(any[ItemCommit])
              .returns(mock[WriteResult].getLastError
                .returns(mock[CommandResult].ok returns true))

            m.findByIdAndVersion(any[ObjectId], any[Long]) returns Seq.empty
            m
          }

          override val itemService: ItemService = {
            mock[ItemService].save(any[Item], any[Boolean]) returns Right(item.id)
          }

          override val assets: ItemDraftAssets = {
            val m = mock[ItemDraftAssets]
            m.copyDraftToItem(any[ObjectId], any[VersionedId[ObjectId]]) answers { (arr, mock) =>
              val array = arr.asInstanceOf[Array[Any]]
              Success(array(1).asInstanceOf[VersionedId[ObjectId]])
            }
            m.deleteDraft(any[ObjectId]) answers { (id) => Success(id.asInstanceOf[ObjectId]) }
            m
          }
        }
      }

      "not allow ed to save gwen's commit" in new scp {
        drafts.commit(ed)(gwensDraft) must_== Failure(UserCantCommit(ed, gwen))
      }

      "allow gwen to save gwen's commit" in new scp {
        drafts.commit(gwen)(gwensDraft) match {
          case Success(ItemCommit(srcId, newId, user, _)) => {
            srcId must_== item.id
            user must_== gwen
            there was one(drafts.assets).copyDraftToItem(gwensDraft.id, item.id)
            there was one(drafts.assets).deleteDraft(gwensDraft.id)
            there was one(drafts.itemService).save(item.copy(id = item.id.copy(version = None)), true)
          }
          case _ => failure("should have got an item commit")
        }
      }
    }
  }
}
