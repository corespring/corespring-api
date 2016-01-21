package org.corespring.v2.api.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.errors.{ GeneralError, DeleteDraftFailed, SaveCommitFailed, DraftError }
import org.corespring.drafts.item.models._
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ FieldValue, Item }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.drafts.item.json.ItemDraftJson
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scalaz.Scalaz._
import scalaz.Success

class ItemDraftsTest
  extends Specification
  with Mockito {

  trait scope extends Scope {

    lazy val jsonFormatting = new JsonFormatting {
      override def findStandardByDotNotation: (String) => Option[Standard] = _ => None

      override def rootOrgId: ObjectId = ObjectId.get

      override def fieldValue: FieldValue = FieldValue()

      override def findSubjectById: (ObjectId) => Option[Subject] = _ => None
    }

    lazy val itemDraftJson = new ItemDraftJson(jsonFormatting)

    lazy val draftId = DraftId(ObjectId.get, "", ObjectId.get)
    lazy val orgAndUser: OrgAndUser = OrgAndUser(SimpleOrg(ObjectId.get, "org"), None)
    lazy val itemDraft = {
      ItemDraft(draftId, Item(collectionId = ObjectId.get.toString), orgAndUser)
    }

    lazy val drafts = {
      val m = mock[org.corespring.drafts.item.ItemDrafts]
      m.listByItemAndOrgId(any[VersionedId[ObjectId]], any[ObjectId]) returns Nil
      m.create(any[DraftId], any[OrgAndUser], any[Option[DateTime]]) returns Success(itemDraft)
      m.load(any[OrgAndUser])(any[DraftId]) returns Success(itemDraft)
      m.loadOrCreate(any[OrgAndUser])(any[DraftId], any[Boolean]) returns Success(itemDraft)
      m
    }

    lazy val userResult: Option[OrgAndUser] = Some(orgAndUser)

    def identifyUser(r: RequestHeader): Option[OrgAndUser] = userResult

    val itemDrafts = new ItemDrafts(drafts, identifyUser, itemDraftJson)
  }

  def TestError(msg: String = "item-drafts-test error") = GeneralError(msg)

  val user = OrgAndUser(
    SimpleOrg(ObjectId.get, "test-org"),
    Some(SimpleUser(ObjectId.get, "ed", "provider", "ed eustace", ObjectId.get)))

  val req = FakeRequest("", "")

  val itemId = VersionedId(ObjectId.get, Some(0))
  val draftId = DraftId(itemId.id, user.user.map(_.userName).getOrElse("test_user"), user.org.id)
  //    val mockItemDraft = ItemDraft(draftId, Item(id = VersionedId(ObjectId.get, Some(1))), user)

  "list" should {

    "fail if no user is found" in new scope {
      override lazy val userResult = None
      val result = itemDrafts.listByItem("")(req)
      contentAsJson(result) must_== AuthenticationFailed.json
    }

    "return error if id is bad" in new scope {
      val result = itemDrafts.listByItem("?")(req)
      status(result) must_== BAD_REQUEST
    }

    "return a list" in new scope {
      val result = itemDrafts.listByItem(itemId.toString)(req)
      status(result) must_== OK
    }
  }

  "create" should {
    //      class scp(user: Option[OrgAndUser] = None,
    //        createResult: Validation[DraftError, ItemDraft] = Failure(TestError("create")),
    //        loadResult: Validation[DraftError, ItemDraft] = Failure(TestError("load"))) extends Scope with TestController {
    //        override def identifyUser(rh: RequestHeader) = user
    //        mockDrafts.create(any[DraftId], any[OrgAndUser], any[Option[DateTime]]) returns createResult
    //        mockDrafts.load(any[OrgAndUser])(any[DraftId]) returns loadResult
    //      }

    "return error if id is bad" in new scope {
      val result = itemDrafts.create("?")(req)
      status(result) must_== BAD_REQUEST
    }

    "returns draft creation failed error" in new scope {
      val result = itemDrafts.create(itemId.toString)(req)
      val err = draftCreationFailed(itemId.toString)
      drafts.create(any[DraftId], any[Org])
      status(result) must_== err.statusCode
    }

    //      Success(
    //        ItemDraft(
    //          DraftId(itemId.id, "?", orgId = ObjectId.get),
    //          Item(id = itemId), user))) {
    "returns ok" in new scope {
      val result = itemDrafts.create(itemId.toString)(req)
      status(result) must_== OK
    }
  }

  "commit" should {

    //      class scp(user: Option[OrgAndUser] = None,
    //        loadResult: Validation[DraftError, ItemDraft] = Failure(TestError("load")),
    //        commitResult: Validation[DraftError, ItemCommit] = Failure(SaveCommitFailed))
    //        extends Scope
    //        with TestController {
    //        override def identifyUser(rh: RequestHeader) = user
    //        mockDrafts.load(any[OrgAndUser])(any[DraftId]) returns loadResult
    //        mockDrafts.commit(any[OrgAndUser])(any[ItemDraft], any[Boolean]).returns(commitResult)
    //      }

    "fail if no user is found" in new scope {
      val result = itemDrafts.commit(draftId.toIdString)(req)
      contentAsJson(result) must_== AuthenticationFailed.json
    }

    "fail if draft is not loaded" in new scope {
      val result = itemDrafts.commit(draftId.toIdString)(req)
      contentAsJson(result) must_== generalDraftApiError("load").json //cantLoadDraft(draftId.toIdString).json
    }

    "fail if commit fails" in new scope {
      val result = itemDrafts.commit(draftId.toIdString)(req)
      contentAsJson(result) must_== generalDraftApiError(SaveCommitFailed.msg).json
    }

    //      Success(mockItemDraft),
    //      Success(ItemCommit(draftId, user, itemId, DateTime.now))) {
    "work if commit is returned" in new scope {
      val result = itemDrafts.commit(draftId.toIdString)(req)
      status(result) must_== OK
    }
  }

  "get" should {
    //      class scp(user: Option[OrgAndUser] = None, loadResult: Validation[DraftError, ItemDraft] = Failure(TestError("load")))
    //        extends Scope
    //        with TestController {
    //        override def identifyUser(rh: RequestHeader) = user
    //        mockDrafts.loadOrCreate(any[OrgAndUser])(any[DraftId], any[Boolean]).returns(loadResult)
    //      }

    "fail if no user is found" in new scope {
      val result = itemDrafts.get(draftId.toIdString)(req)
      contentAsJson(result) must_== AuthenticationFailed.json
    }

    "fail if draft loading fails" in new scope {
      contentAsJson(itemDrafts.get(draftId.toIdString)(req)) must_== generalDraftApiError("load").json
    }

    s"return $OK" in new scope {
      status(itemDrafts.get(draftId.toIdString)(req)) must_== OK
    }
  }

  "save" should {
    s"Do we allow an api save of a draft?" in pending
  }

  "delete" should {

    //      class scp(user: Option[OrgAndUser] = None,
    //        deleteResult: Validation[DraftError, DraftId] = Failure(DeleteDraftFailed(draftId)))
    //        extends Scope
    //        with TestController {
    //        override def identifyUser(rh: RequestHeader) = user
    //        //itemDrafts.remove(any[OrgAndUser])(any[DraftId]).returns(deleteResult)
    //      }

    "fail if no user is found" in new scope {
      contentAsJson(itemDrafts.delete(draftId.toIdString, None)(req)) must_== AuthenticationFailed.json
    }

    "fail if delete fails" in new scope {
      contentAsJson(itemDrafts.delete(draftId.toIdString, None)(req)) must_== generalDraftApiError(DeleteDraftFailed(draftId).msg).json
    }

    s"return $OK" in new scope {
      status(itemDrafts.delete(draftId.toIdString, None)(req)) must_== OK
    }
  }

}
