package org.corespring.v2.player.hooks

import java.util.concurrent.TimeUnit

import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.drafts.errors.{ DraftError, GeneralError }
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.{ ItemDrafts, S3Paths }
import org.corespring.models.appConfig.Bucket
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.models.{ User, UserOrg }
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scalaz.{ Failure, Success, Validation }

class DraftEditorHooksTest extends Specification with Mockito with MockFactory {

  val item = Item(collectionId = ObjectId.get.toString)
  val orgId = ObjectId.get

  def TestError(name: String) = GeneralError(name)

  class scope(
    val loadOrCreateResult: Validation[DraftError, ItemDraft] = Failure(TestError("load or create"))) extends Scope {

    val itemId = ObjectId.get

    def transform: (Item) => JsValue = i => Json.obj()

    val playS3 = {
      val m = mock[S3Service]
      m
    }

    val itemDrafts = {
      val m = mock[ItemDrafts]
      m.loadOrCreate(any[OrgAndUser])(any[DraftId], any[Boolean]) returns {
        val loadOrCreateResult: Validation[DraftError, ItemDraft] = Failure(TestError("load or create"))
        loadOrCreateResult
      }
      m
    }

    val itemTransformer = {
      val m = mock[ItemTransformer]
      m
    }

    val orgAndOpts: Validation[V2Error, OrgAndOpts] = {
      val out = mockOrgAndOpts(AuthMode.AccessToken)
      out.copy(user = Some(User(userName = "ed", org = UserOrg(orgId, Permission.Write.value))))
      Success(out)
    }

    def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

    val hooks = new DraftEditorHooks(
      itemTransformer,
      playS3,
      Bucket("bucket"),
      itemDrafts,
      getOrgAndOptions)

    def orgAndUser(oo: OrgAndOpts) = {
      OrgAndUser(SimpleOrg.fromOrganization(oo.org), oo.user.map(SimpleUser.fromUser))
    }

    def futureResult[A](f: Future[A]) = Await.result[A](f, Duration(1, TimeUnit.SECONDS))
  }

  "load" should {
    "call loadOrCreate" in new scope {
      val ou = orgAndUser(orgAndOpts.toOption.get)
      val draftId = hooks.mkDraftId(ou, s"$itemId:0").toOption.get
      override lazy val loadOrCreateResult: Validation[DraftError, ItemDraft] = Success(ItemDraft(draftId, item, ou))

      val f = hooks.load(s"$itemId:0")(FakeRequest("", ""))
      val r = futureResult(f)

      r match {
        case Left((code, msg)) => failure(s"got: $msg")
        case Right(json) => {
          there was one(itemDrafts).loadOrCreate(ou)(draftId, true)
        }
      }
    }
  }

  "loadFile" should {
    "call s3.download" in new scope {
      val ou = orgAndUser(orgAndOpts.toOption.get)
      val draftId = hooks.mkDraftId(ou, s"$itemId:0").toOption.get
      hooks.loadFile(s"$itemId:0", "file")(FakeRequest("", ""))
      there was one(playS3).download("bucket", S3Paths.draftFile(draftId, "file"))
    }
  }

  "deleteFile" should {
    "call s3.delete" in new scope {

      playS3.delete(any[String], any[String]) returns {
        mock[DeleteResponse].success.returns(true)
      }

      itemDrafts.owns(any[OrgAndUser])(any[DraftId]) returns true

      val ou = orgAndUser(orgAndOpts.toOption.get)
      val draftId = hooks.mkDraftId(ou, s"$itemId:0").toOption.get
      val f = hooks.deleteFile(s"$itemId:0", "file")(FakeRequest("", ""))
      val r = futureResult(f)
      there was one(playS3).delete("bucket", S3Paths.draftFile(draftId, "file"))
    }
  }
}
