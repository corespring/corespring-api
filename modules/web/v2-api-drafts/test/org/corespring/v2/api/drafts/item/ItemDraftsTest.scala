package org.corespring.v2.api.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.errors.{ GeneralError, DeleteDraftFailed, SaveCommitFailed, DraftError }
import org.corespring.drafts.item
import org.corespring.drafts.item.models._
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test.{ PlaySpecification, FakeRequest }

import scalaz.{ Success, Failure, Validation }

class ItemDraftsTest extends Specification with PlaySpecification with Mockito {

  PlaySingleton.start()

  trait TestController extends ItemDrafts {

    val mockDrafts = mock[item.ItemDrafts]

    override def drafts: item.ItemDrafts = mockDrafts

    override def identifyUser(rh: RequestHeader): Option[OrgAndUser] = None
  }

  def TestError(msg: String = "item-drafts-test error") = GeneralError(msg)

  val user = OrgAndUser(
    SimpleOrg(ObjectId.get, "test-org"),
    Some(SimpleUser(ObjectId.get, "ed", "provider", "ed eustace", ObjectId.get)))

  "ItemDrafts" should {

    val req = FakeRequest("", "")

    val itemId = VersionedId(ObjectId.get, Some(0))
    val draftId = DraftId.fromIdAndUser(itemId, user)
    val mockItemDraft = ItemDraft(Item(id = VersionedId(ObjectId.get, Some(1))), user)

    "list" should {

      class scp(user: Option[OrgAndUser] = None) extends Scope with TestController {
        override def identifyUser(rh: RequestHeader) = user
        mockDrafts.listByItemAndOrgId(any[VersionedId[ObjectId]], any[ObjectId]) returns Seq.empty
      }

      "fail if no user is found" in new scp {
        val result = listByItem("")(req)
        contentAsJson(result) must_== AuthenticationFailed.json
      }

      "return error if id is bad" in new scp(Some(user)) {
        val result = listByItem("?")(req)
        status(result) === BAD_REQUEST
      }

      "return a list" in new scp(Some(user)) {
        val result = listByItem(itemId.toString)(req)
        status(result) === OK
      }
    }

    "create" should {
      class scp(user: Option[OrgAndUser] = None,
        createResult: Validation[DraftError, ItemDraft] = Failure(TestError("create")),
        loadResult: Validation[DraftError, ItemDraft] = Failure(TestError("load"))) extends Scope with TestController {
        override def identifyUser(rh: RequestHeader) = user
        mockDrafts.create(any[VersionedId[ObjectId]], any[OrgAndUser], any[Option[DateTime]]) returns createResult
        mockDrafts.load(any[OrgAndUser])(any[DraftId]) returns loadResult
      }

      "return error if id is bad" in new scp(Some(user)) {
        val result = create("?")(req)
        status(result) === BAD_REQUEST
      }

      "returns draft creation failed error" in new scp(Some(user)) {
        val result = create(itemId.toString)(req)
        val err = draftCreationFailed(itemId.toString)
        status(result) === err.statusCode
      }

      "returns ok" in new scp(Some(user),
        Success(
          ItemDraft(
            Item(id = VersionedId(ObjectId.get, Some(1))), user))) {
        val result = create(itemId.toString)(req)
        status(result) === OK
      }
    }

    "commit" should {

      class scp(user: Option[OrgAndUser] = None,
        loadResult: Validation[DraftError, ItemDraft] = Failure(TestError("load")),
        commitResult: Validation[DraftError, ItemCommit] = Failure(SaveCommitFailed))
        extends Scope
        with TestController {
        override def identifyUser(rh: RequestHeader) = user
        mockDrafts.load(any[OrgAndUser])(any[DraftId]) returns loadResult
        mockDrafts.commit(any[OrgAndUser])(any[ItemDraft], any[Boolean]).returns(commitResult)
      }

      "fail if no user is found" in new scp {
        val result = commit(draftId.toIdString)(req)
        contentAsJson(result) must_== AuthenticationFailed.json
      }

      "fail if draft is not loaded" in new scp(Some(user)) {
        val result = commit(draftId.toIdString)(req)
        contentAsJson(result) must_== generalDraftApiError("load").json //cantLoadDraft(draftId.toIdString).json
      }

      "fail if commit fails" in new scp(Some(user), Success(mockItemDraft)) {
        val result = commit(draftId.toIdString)(req)
        contentAsJson(result) must_== generalDraftApiError(SaveCommitFailed.msg).json
      }

      "work if commit is returned" in new scp(Some(user),
        Success(mockItemDraft),
        Success(ItemCommit(draftId, user, itemId, DateTime.now))) {
        val result = commit(draftId.toIdString)(req)
        status(result) must_== OK
      }
    }

    "get" should {
      class scp(user: Option[OrgAndUser] = None, loadResult: Validation[DraftError, ItemDraft] = Failure(TestError("load")))
        extends Scope
        with TestController {
        override def identifyUser(rh: RequestHeader) = user
        mockDrafts.loadOrCreate(any[OrgAndUser])(any[DraftId]).returns(loadResult)
      }

      "fail if no user is found" in new scp {
        val result = get(draftId.toIdString)(req)
        contentAsJson(result) must_== AuthenticationFailed.json
      }

      "fail if draft loading fails" in new scp(Some(user)) {
        contentAsJson(get(draftId.toIdString)(req)) === cantLoadDraft(draftId.toIdString).json
      }

      s"return $OK" in new scp(Some(user), Success(mockItemDraft)) {
        status(get(draftId.toIdString)(req)) === OK
      }
    }

    "save" should {
      s"return $OK" in { true === false }.pendingUntilFixed
    }

    "delete" should {

      class scp(user: Option[OrgAndUser] = None,
        deleteResult: Validation[DraftError, DraftId] = Failure(DeleteDraftFailed(draftId)))
        extends Scope
        with TestController {
        override def identifyUser(rh: RequestHeader) = user
        mockDrafts.remove(any[OrgAndUser])(any[DraftId]).returns(deleteResult)
      }

      "fail if no user is found" in new scp {
        contentAsJson(delete(draftId.toIdString)(req)) === AuthenticationFailed.json
      }

      "fail if delete fails" in new scp(Some(user)) {
        contentAsJson(delete(draftId.toIdString)(req)) === generalDraftApiError(DeleteDraftFailed(draftId).msg).json
      }

      s"return $OK" in new scp(Some(user), Success(draftId)) {
        status(delete(draftId.toIdString)(req)) === OK
      }
    }

  }
}
