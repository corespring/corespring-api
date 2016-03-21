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
import scalaz.{ Failure, Success }

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

    lazy val itemId = ObjectId.get
    lazy val draftId = DraftId(itemId, "name", ObjectId.get)
    lazy val orgAndUser: OrgAndUser = OrgAndUser(SimpleOrg(ObjectId.get, "org"), None)
    lazy val itemDraft = {
      ItemDraft(draftId, Item(collectionId = ObjectId.get.toString), orgAndUser)
    }

    lazy val itemCommit = ItemCommit(draftId, orgAndUser, VersionedId(itemId, Some(1)), DateTime.now)

    lazy val drafts = {
      val m = mock[org.corespring.drafts.item.ItemDrafts]
      m.listByItemAndOrgId(any[VersionedId[ObjectId]], any[ObjectId]) returns Nil
      m.create(any[DraftId], any[OrgAndUser], any[Option[DateTime]]) returns Success(itemDraft)
      m.load(any[OrgAndUser])(any[DraftId]) returns Success(itemDraft)
      m.loadOrCreate(any[OrgAndUser])(any[DraftId], any[Boolean]) returns Success(itemDraft)
      m.commit(any[OrgAndUser])(any[ItemDraft], any[Boolean]) returns Success(itemCommit)
      m.remove(any[OrgAndUser])(any[DraftId], any[Boolean]) returns Success(draftId)
      m
    }

    lazy val userResult: Option[OrgAndUser] = Some(orgAndUser)

    def identifyUser(r: RequestHeader): Option[OrgAndUser] = userResult

    val itemDrafts = new ItemDrafts(drafts, identifyUser, itemDraftJson)
  }

  def TestError(msg: String = "item-drafts-test error") = GeneralError(msg)

  val req = FakeRequest("", "")

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

    "return error if id is bad" in new scope {
      val result = itemDrafts.create("?")(req)
      status(result) must_== BAD_REQUEST
    }

    "returns draft creation failed error" in new scope {
      val err = draftCreationFailed(itemId.toString)
      drafts.create(any[DraftId], any[OrgAndUser], any[Option[DateTime]]) returns Failure(TestError("create-failed"))
      val result = itemDrafts.create(itemId.toString)(req)
      status(result) must_== err.statusCode
    }

    "returns ok" in new scope {
      val result = itemDrafts.create(itemId.toString)(req)
      status(result) must_== OK
    }
  }

  def e(msg: String) = Failure(TestError(msg))

  "commit" should {

    "fail if no user is found" in new scope {
      override lazy val userResult = None
      val result = itemDrafts.commit(draftId.toIdString)(req)
      contentAsJson(result) must_== AuthenticationFailed.json
    }

    "fail if draft is not loaded" in new scope {
      drafts.load(any[OrgAndUser])(any[DraftId]) returns e("load")
      val result = itemDrafts.commit(draftId.toIdString)(req)
      (contentAsJson(result) \ "error").as[String] must_== "load"
    }

    "fail if commit fails" in new scope {
      drafts.commit(any[OrgAndUser])(any[ItemDraft], any[Boolean]) returns e("commit")
      val result = itemDrafts.commit(draftId.toIdString)(req)
      (contentAsJson(result) \ "error").as[String] must_== "commit"
    }

    "work if commit is returned" in new scope {
      val result = itemDrafts.commit(draftId.toIdString)(req)
      status(result) must_== OK
    }
  }

  "get" should {

    "fail if no user is found" in new scope {
      override lazy val userResult = None
      val result = itemDrafts.get(draftId.toIdString)(req)
      contentAsJson(result) must_== AuthenticationFailed.json
    }

    "fail if draft loading fails" in new scope {
      drafts.loadOrCreate(any[OrgAndUser])(any[DraftId], any[Boolean]) returns e("load")
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

    "fail if no user is found" in new scope {
      override lazy val userResult = None
      contentAsJson(itemDrafts.delete(draftId.toIdString, None, None)(req)) must_== AuthenticationFailed.json
    }

    "fail if remove fails" in new scope {
      drafts.remove(any[OrgAndUser])(any[DraftId], any[Boolean]) returns e("delete")
      val result = itemDrafts.delete(draftId.toIdString, None, None)(req)
      (contentAsJson(result) \ "error").as[String] must_== "delete"
    }

    s"return $OK" in new scope {
      status(itemDrafts.delete(draftId.toIdString, None, None)(req)) must_== OK
    }
  }

}
