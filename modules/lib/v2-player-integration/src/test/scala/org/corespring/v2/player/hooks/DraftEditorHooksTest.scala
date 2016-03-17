package org.corespring.v2.player.hooks

import com.amazonaws.services.s3.model.S3Object
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.container.client.hooks.UploadResult
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.drafts.errors.DraftError
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.{ ItemDrafts, S3Paths }
import org.corespring.models.appConfig.Bucket
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ BaseFile, StoredFile }
import org.corespring.models.{ User, UserOrg }
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.specs2.specification.Scope
import play.api.libs.iteratee.{ Input, Iteratee, Step }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ BodyParser, RequestHeader, SimpleResult }
import play.api.test.FakeRequest
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{ Await, ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

trait BodyParserHelper {

  def mkBodyParser[A](r: A): BodyParser[A] = BodyParser.apply { rh =>
    val o: Iteratee[Array[Byte], Either[SimpleResult, A]] = new Iteratee[Array[Byte], Either[SimpleResult, A]] {
      override def fold[B](folder: (Step[Array[Byte], Either[SimpleResult, A]]) => Future[B])(implicit ec: ExecutionContext): Future[B] = {
        folder(Step.Done(Right(r), Input.Empty))
      }
    }
    o
  }
}
class DraftEditorHooksTest extends V2PlayerIntegrationSpec {

  val item = Item(collectionId = ObjectId.get.toString)
  val orgId = ObjectId.get

  trait scope extends Scope with BodyParserHelper {

    lazy val loadOrCreateResult: Validation[DraftError, ItemDraft] = Failure(DraftTestError("load or create"))

    val itemId = ObjectId.get

    val playS3 = {
      val m = mock[S3Service]

      m.s3ObjectAndData[ItemDraft](any[String], any[ItemDraft => String])(any[RequestHeader => Either[SimpleResult, ItemDraft]]) returns {
        val s3o = mock[S3Object]
        s3o.getKey returns "s3-key.png"
        mkBodyParser(Future.successful(s3o, mock[ItemDraft]))
      }
      m
    }

    lazy val itemDrafts = {
      val m = mock[ItemDrafts]
      m.loadOrCreate(any[OrgAndUser])(any[DraftId], any[Boolean]) returns loadOrCreateResult
      m.addFileToChangeSet(any[ItemDraft], any[StoredFile]) returns true
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

  "upload" should {

    trait upload extends scope {

      def predicate(r: RequestHeader): Option[SimpleResult] = None
      lazy val bp = hooks.upload("id", "path")(predicate)
      lazy val eitherResult = bp(FakeRequest()).run
      lazy val out = eitherResult.flatMap { e =>
        e.fold(_ => Future.successful(None), r => r.map(Some(_)))
      }
    }

    "returns the path param in an UploadResult" in new upload {
      out must equalTo(Some(UploadResult("path"))).await
    }

    "calls drafts.addFileToChangeSet" in new upload {
      await(out)
      there was one(itemDrafts).addFileToChangeSet(any[ItemDraft], any[StoredFile])
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

    trait deleteFileScope extends scope {
      playS3.delete(any[String], any[String]) returns {
        mock[DeleteResponse].success.returns(true)
      }

      itemDrafts.owns(any[OrgAndUser])(any[DraftId]) returns true

      val filename = "file.jpeg"

      val ou = orgAndUser(orgAndOpts.toOption.get)
      val draftId = hooks.mkDraftId(ou, s"$itemId:0").toOption.get
      val f = hooks.deleteFile(s"$itemId:0", filename)(FakeRequest("", ""))
      val r = await(f)
    }

    "call s3.delete" in new deleteFileScope {
      there was one(playS3).delete("bucket", S3Paths.draftFile(draftId, filename))
    }

    "call itemDrafts.removeFileFromChangeSet" in new deleteFileScope {
      val storedFile = StoredFile(filename, BaseFile.getContentType(filename), false, filename)
      there was one(itemDrafts).removeFileFromChangeSet(draftId, storedFile)
    }
  }
}
