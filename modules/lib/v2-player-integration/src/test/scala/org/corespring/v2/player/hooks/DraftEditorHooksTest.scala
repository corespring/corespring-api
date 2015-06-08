package org.corespring.v2.player.hooks

import java.util.concurrent.TimeUnit

import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.drafts.errors.{ GeneralError, DraftError }
import org.corespring.drafts.item.{ S3Paths, ItemDrafts }
import org.corespring.drafts.item.models._
import org.corespring.platform.core.models.User
import org.corespring.platform.core.models.item.Item
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.models.{ MockFactory, AuthMode, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.Duration
import scalaz.{ Failure, Success, Validation }

class DraftEditorHooksTest extends Specification with Mockito with MockFactory {

  PlaySingleton.start

  def TestError(name: String) = GeneralError(name)

  class scope() extends Scope with DraftEditorHooks {

    val orgAndOpts: Validation[V2Error, OrgAndOpts] = {
      val out = mockOrgAndOpts(AuthMode.AccessToken)
      out.copy(user = Some(User(userName = "ed")))
      Success(out)
    }

    lazy val loadOrCreateResult: Validation[DraftError, ItemDraft] = Failure(TestError("load or create"))

    val itemId = ObjectId.get

    def orgAndUser(oo: OrgAndOpts) = {
      OrgAndUser(SimpleOrg.fromOrganization(oo.org), oo.user.map(SimpleUser.fromUser))
    }

    val mockS3 = {
      val m = mock[S3Service]
      m
    }

    val mockItemDrafts = {
      val m = mock[ItemDrafts]
      m.loadOrCreate(any[OrgAndUser])(any[DraftId], any[Boolean]) returns {
        println(s"return $loadOrCreateResult")
        loadOrCreateResult
      }
      m
    }

    override def transform: (Item) => JsValue = i => Json.obj()

    override def playS3: S3Service = mockS3

    override def backend: ItemDrafts = mockItemDrafts

    override def bucket: String = "bucket"

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

    def futureResult[A](f: Future[A]) = Await.result[A](f, Duration(1, TimeUnit.SECONDS))
  }

  "load" should {
    "call loadOrCreate" in new scope {
      val ou = orgAndUser(orgAndOpts.toOption.get)
      val draftId = mkDraftId(ou, s"$itemId:0").toOption.get
      override lazy val loadOrCreateResult: Validation[DraftError, ItemDraft] = Success(ItemDraft(draftId, Item(), ou))

      val f = load(s"$itemId:0")(FakeRequest("", ""))
      val r = futureResult(f)

      r match {
        case Left((code, msg)) => failure(s"got: $msg")
        case Right(json) => {
          there was one(mockItemDrafts).loadOrCreate(ou)(draftId, true)
        }
      }
    }
  }

  "loadFile" should {
    "call s3.download" in new scope {
      val ou = orgAndUser(orgAndOpts.toOption.get)
      val draftId = mkDraftId(ou, s"$itemId:0").toOption.get
      loadFile(s"$itemId:0", "file")(FakeRequest("", ""))
      there was one(mockS3).download("bucket", S3Paths.draftFile(draftId, "file"))
    }
  }

  "deleteFile" should {
    "call s3.delete" in new scope {

      mockS3.delete(any[String], any[String]) returns {
        mock[DeleteResponse].success.returns(true)
      }

      mockItemDrafts.owns(any[OrgAndUser])(any[DraftId]) returns true

      val ou = orgAndUser(orgAndOpts.toOption.get)
      val draftId = mkDraftId(ou, s"$itemId:0").toOption.get
      val f = deleteFile(s"$itemId:0", "file")(FakeRequest("", ""))
      val r = futureResult(f)
      there was one(mockS3).delete("bucket", S3Paths.draftFile(draftId, "file"))
    }
  }
}
