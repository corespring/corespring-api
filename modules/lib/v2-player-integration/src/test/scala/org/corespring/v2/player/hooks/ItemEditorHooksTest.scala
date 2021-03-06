package org.corespring.v2.player.hooks

import com.amazonaws.services.s3.model.S3Object
import org.apache.commons.httpclient.util.URIUtil
import org.bson.types.ObjectId
import org.corespring.amazon.s3.{ S3Service, Uploaded }
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.common.url.EncodingHelper
import org.corespring.container.client.hooks.UploadResult
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.drafts.item.S3Paths
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ BaseFile, StoredFile }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts }
import org.corespring.v2.errors.Errors.cantParseItemId
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.specs2.specification.Scope
import play.api.libs.iteratee.{ Input, Iteratee, Step }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.test.FakeRequest

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }
import scala.concurrent.duration._

class ItemEditorHooksTest extends V2PlayerIntegrationSpec {

  val mockOrgAndOptsForSpecs = mockOrgAndOpts(AuthMode.AccessToken)
  val vid = VersionedId(ObjectId.get, Some(0))
  val vidNoVersion = vid.copy(version = None)
  val encodingHelper = new EncodingHelper();

  def urlEncode(s: String): String = {
    encodingHelper.encodedOnce(s)
  }

  private class scope(
    val transformResult: JsValue = Json.obj(),
    val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOptsForSpecs)) extends Scope with BodyParserHelper {

    lazy val itemService = mock[ItemService]

    val uploaded = Uploaded("bucket", "some/path.png")

    val item = Item(collectionId = ObjectId.get.toString)

    lazy val playS3 = {
      val m = mock[S3Service]
      m.download(any[String], any[String], any[Option[Headers]]) returns Results.Ok("ok")
      m.delete(any[String], any[String]) returns {
        val r = mock[DeleteResponse]
        r.success returns true
        r
      }

      m.uploadWithData(any[String], any[Item => String])(any[RequestHeader => Either[SimpleResult, Item]]) returns {
        mkBodyParser(Future.successful(uploaded, item))
      }
      m
    }

    lazy val itemAuth = {
      val m = mock[ItemAuth[OrgAndOpts]]
      m.loadForWrite(any[String])(any[OrgAndOpts]) returns Success(mockItem)
      m.canWrite(any[String])(any[OrgAndOpts]) returns Success(true)
      m
    }

    lazy val itemTransformer = {
      val m = mock[ItemTransformer]
      m.transformToV2Json(any[Item]) returns transformResult
      m
    }

    def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

    val hooks = new ItemEditorHooks(
      itemTransformer,
      playS3,
      Bucket("bucket"),
      itemAuth,
      itemService,
      getOrgAndOptions,
      containerExecutionContext)
  }

  import scala.concurrent.duration._

  "load" should {
    "load the item" in new scope {
      hooks.load("itemId").map(_.isRight) must equalTo(true).await(timeout = 2.seconds)
    }

    "load calls transform" in new scope(transformResult = Json.obj("transformed" -> true)) {
      hooks.load("itemId").map(_.right.get._1) must equalTo(transformResult).await(timeout = 2.seconds)
    }

    "return the itemAuth.loadForWrite error" in new scope() {
      val err = TestError("itemAuth.loadForWrite")
      itemAuth.loadForWrite(any[String])(any[OrgAndOpts]) returns Failure(err)
      hooks.load("itemId").map(_.left.get) must equalTo((err.statusCode, err.message)).await(timeout = 2.seconds)
    }

    "return the org error" in new scope(orgAndOpts = Failure(TestError("org and opts"))) {
      val err = TestError("org and opts")
      hooks.load("itemId").map(_.left.get) must equalTo((err.statusCode, err.message)).await(timeout = 2.seconds)
    }
  }

  "loadFile" should {
    "call s3 download" in new scope() {
      val result = hooks.loadFile(vid.toString, "path")(fakeRequest)
      there was one(playS3).download("bucket", S3Paths.itemFile(vid, "path"), null)
    }

    "return cantParseItemId error" in new scope {
      val result = hooks.loadFile("bad id", "path")(fakeRequest)
      val err = cantParseItemId("bad id")
      status(Future(result)) === err.statusCode
      contentAsString(Future(result)) === err.message
    }

    "default to latest version when itemid has no version" in new scope() {
      val result = hooks.loadFile(vidNoVersion.toString, "path")(fakeRequest)
      there was one(playS3).download("bucket", S3Paths.itemFile(vid, "path"), null)
    }

  }

  "deleteFile" should {

    "call s3.delete" in new scope {
      await(hooks.deleteFile(vid.toString, "path"))
      there was one(playS3).delete("bucket", S3Paths.itemFile(vid, "path"))
    }

    "returns the s3 DeleteResponse message" in new scope {
      playS3.delete(any[String], any[String]) returns {
        val r = mock[DeleteResponse]
        r.success returns false
        r.msg returns "s3 error"
        r
      }

      val result = await(hooks.deleteFile(vid.toString, "path"))
      result === Some(BAD_REQUEST, "s3 error")
    }

    "call itemService.removeFileFromPlayerDefinition" in new scope {
      val mockItem = Item(collectionId = ObjectId.get.toString)
      val mockKey = "some/mock-key.png"

      playS3.delete(any[String], any[String]) returns {
        val r = mock[DeleteResponse]
        r.success returns true
        r
      }

      await(hooks.deleteFile(mockItem.id.toString, mockKey))
      val file = StoredFile(mockKey, BaseFile.getContentType(mockKey), false, grizzled.file.util.basename(mockKey))
      there was one(itemService).removeFileFromPlayerDefinition(mockItem.id, file)
    }

  }

  "upload" should {

    trait upload extends scope with BodyParserHelper {
      val mockItem = Item(collectionId = ObjectId.get.toString)
      val mockKey = "some/mock-key with a space%20.png"

      playS3.uploadWithData(any[String], any[Item => String])(any[RequestHeader => Either[SimpleResult, Item]]) returns {
        mkBodyParser(Future.successful(Uploaded("bucket", mockKey), mockItem))
      }
      lazy val bp = hooks.upload(vid.toString, mockKey)((rh: RequestHeader) => None)
      lazy val eitherResult = bp(FakeRequest()).run
      lazy val out = eitherResult.flatMap { e =>
        e.fold(_ => Future.successful(None), r => r.map(Some(_)))
      }
    }

    "returns the path in an UploadResult" in new upload {
      out must equalTo(Some(UploadResult(urlEncode(mockKey)))).await
    }

    "makeKey should return the full s3 key" in new upload {
      val captor = capture[Item => String]
      val expectedKey = urlEncode(mockKey)
      out must equalTo(Some(UploadResult(expectedKey))).await
      there was one(playS3).uploadWithData(any[String], captor)(any[RequestHeader => Either[SimpleResult, Item]])
      captor.value.apply(item) must_== urlEncode(S3Paths.itemFile(item.id, mockKey))
    }

    "call itemService.addFileToPlayerDefinition" in new upload {
      await(out)
      val file = StoredFile(mockKey, BaseFile.getContentType(mockKey), false, grizzled.file.util.basename(mockKey))
      there was one(itemService).addFileToPlayerDefinition(mockItem, file)
    }
  }
}
