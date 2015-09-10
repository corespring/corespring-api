package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.drafts.errors.DraftError
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.{ ItemDrafts, S3Paths }
import org.corespring.models.appConfig.Bucket
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.models.{ User, UserOrg }
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scalaz.{ Failure, Success, Validation }

class DraftEditorHooksTest extends V2PlayerIntegrationSpec {

  implicit val ec = containerExecutionContext

  val item = Item(collectionId = ObjectId.get.toString)
  val orgId = ObjectId.get

  trait scope extends Scope {
    lazy val loadOrCreateResult: Validation[DraftError, ItemDraft] = Failure(DraftTestError("load or create"))

    val itemId = ObjectId.get

    val playS3 = {
      val m = mock[S3Service]
      m
    }

    lazy val itemDrafts = {
      val m = mock[ItemDrafts]
      m.loadOrCreate(any[OrgAndUser])(any[DraftId], any[Boolean]) returns loadOrCreateResult
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
      getOrgAndOptions,
      containerExecutionContext)

    def orgAndUser(oo: OrgAndOpts) = {
      OrgAndUser(SimpleOrg.fromOrganization(oo.org), oo.user.map(SimpleUser.fromUser))
    }
  }

  "load" should {
    "call loadOrCreate" in new scope {
      override lazy val loadOrCreateResult = Success(ItemDraft(draftId, item, ou))
      val ou = orgAndUser(orgAndOpts.toOption.get)
      val draftId = hooks.mkDraftId(ou, s"$itemId:0").toOption.get

      val f = hooks.load(s"$itemId:0")(FakeRequest("", ""))

      f.map(_.isRight) must equalTo(true).await
      there was one(itemDrafts).loadOrCreate(ou)(draftId, true)
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
      val r = waitFor(f)
      there was one(playS3).delete("bucket", S3Paths.draftFile(draftId, "file"))
    }
  }
}
