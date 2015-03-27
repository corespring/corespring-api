package org.corespring.drafts.item

import com.mongodb.{ CommandResult, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.errors.UserCantSave
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
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
    val item = Item()

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

  }
}
