package org.corespring.v2.api.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.errors.{ DeleteDraftFailed, SaveCommitFailed, DraftError }
import org.corespring.drafts.item
import org.corespring.drafts.item.models.{ ItemCommit, ItemDraft, SimpleOrg, OrgAndUser }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test.{ PlaySpecification, FakeRequest }

import scalaz.{ Success, Failure, Validation }

class ItemDraftsTest extends Specification with PlaySpecification with Mockito {

  trait TestController extends ItemDrafts {
    override def drafts: item.ItemDrafts = ???

    override def identifyUser(rh: RequestHeader): Option[OrgAndUser] = ???
  }

  val user = OrgAndUser(SimpleOrg(ObjectId.get, "test-org"), None)

  "ItemDrafts" should {

    val req = FakeRequest("", "")

    val itemId = VersionedId(ObjectId.get, Some(0))
    val draftId = ObjectId.get
    val mockItemDraft = ItemDraft(Item(id = VersionedId(ObjectId.get, Some(1))), user)

    "list" should {

      class scp(user: Option[OrgAndUser] = None) extends Scope {
        val controller = new TestController {
          override def identifyUser(rh: RequestHeader) = user

          override val drafts: item.ItemDrafts = {
            mock[item.ItemDrafts].list(any[VersionedId[ObjectId]]) returns Seq.empty
          }
        }
      }

      "fail if no user is found" in new scp {
        val result = controller.commit(draftId)(req)
        contentAsJson(result) must_== AuthenticationFailed.json
      }

      "return error if id is bad" in new scp(Some(user)) {
        val result = controller.list("?")(req)
        status(result) === BAD_REQUEST
      }

      "return a list" in new scp(Some(user)) {
        val result = controller.list(itemId.toString)(req)
        status(result) === OK
      }
    }

    "create" should {
      class scp(user: Option[OrgAndUser] = None, createResult: Option[ItemDraft] = None) extends Scope {
        val controller = new TestController {
          override def identifyUser(rh: RequestHeader) = user

          override def drafts: item.ItemDrafts = mock[item.ItemDrafts].create(any[ObjectId], any[OrgAndUser], any[Option[DateTime]]) returns createResult
        }
      }

      "return error if id is bad" in new scp(Some(user)) {
        val result = controller.create("?")(req)
        status(result) === BAD_REQUEST
      }

      "returns draft creation failed error" in new scp(Some(user)) {
        val result = controller.create(itemId.toString)(req)
        val err = draftCreationFailed(itemId.toString)
        status(result) === err.statusCode
      }

      "returns ok" in new scp(Some(user),
        Some(
          ItemDraft(
            Item(id = VersionedId(ObjectId.get, Some(1))), user))) {
        val result = controller.create(itemId.toString)(req)
        status(result) === OK
      }
    }

    "commit" should {

      class scp(user: Option[OrgAndUser] = None,
        loadResult: Option[ItemDraft] = None,
        commitResult: Validation[DraftError, ItemCommit] = Failure(SaveCommitFailed)) extends Scope {
        val controller = new TestController {
          override def identifyUser(rh: RequestHeader) = user

          override def drafts: item.ItemDrafts = {
            val m = mock[item.ItemDrafts]
            m.load(any[OrgAndUser])(any[ObjectId]).returns(loadResult)
            m.commit(any[OrgAndUser])(any[ItemDraft], any[Boolean]).returns(commitResult)
          }
        }
      }

      "fail if no user is found" in new scp {
        val result = controller.commit(draftId)(req)
        contentAsJson(result) must_== AuthenticationFailed.json
      }

      "fail if draft is not loaded" in new scp(Some(user)) {
        val result = controller.commit(draftId)(req)
        contentAsJson(result) must_== cantLoadDraft(draftId).json
      }

      "fail if commit fails" in new scp(Some(user), Some(mockItemDraft)) {
        val result = controller.commit(draftId)(req)
        contentAsJson(result) must_== generalDraftApiError(SaveCommitFailed.msg).json
      }

      "work if commit is returned" in new scp(Some(user), Some(mockItemDraft), Success(ItemCommit(itemId, itemId, user, DateTime.now))) {
        val result = controller.commit(draftId)(req)
        status(result) must_== OK
      }
    }

    "get" should {
      class scp(user: Option[OrgAndUser] = None, loadResult: Option[ItemDraft] = None) extends Scope {
        val controller = new TestController {
          override def identifyUser(rh: RequestHeader) = user

          override def drafts: item.ItemDrafts = {
            val m = mock[item.ItemDrafts]
            m.load(any[OrgAndUser])(any[ObjectId]).returns(loadResult)
          }
        }
      }

      "fail if no user is found" in new scp {
        val result = controller.commit(draftId)(req)
        contentAsJson(result) must_== AuthenticationFailed.json
      }

      "fail if draft loading fails" in new scp(Some(user)) {
        contentAsJson(controller.get(draftId)(req)) === cantLoadDraft(draftId).json
      }

      s"return $OK" in new scp(Some(user), Some(mockItemDraft)) {
        status(controller.get(draftId)(req)) === OK
      }
    }

    "save" should {
      s"return $OK" in { true === false }.pendingUntilFixed
    }

    "delete" should {

      class scp(user: Option[OrgAndUser] = None,
        deleteResult: Validation[DraftError, ObjectId] = Failure(DeleteDraftFailed(draftId))) extends Scope {
        val controller = new TestController {
          override def identifyUser(rh: RequestHeader) = user

          override def drafts: item.ItemDrafts = {
            mock[item.ItemDrafts]
              .removeDraftByIdAndUser(any[ObjectId], any[OrgAndUser]).returns(deleteResult)
          }
        }
      }

      "fail if no user is found" in new scp {
        contentAsJson(controller.delete(draftId)(req)) === AuthenticationFailed.json
      }

      "fail if delete fails" in new scp(Some(user)) {
        contentAsJson(controller.delete(draftId)(req)) === generalDraftApiError(DeleteDraftFailed(draftId).msg).json
      }

      s"return $OK" in new scp(Some(user), Success(ObjectId.get)) {
        status(controller.delete(draftId)(req)) === OK
      }
    }

  }
}
