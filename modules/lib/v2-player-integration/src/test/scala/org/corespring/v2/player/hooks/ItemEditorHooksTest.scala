package org.corespring.v2.player.hooks

import com.amazonaws.services.s3.model.S3Object
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
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

class ItemEditorHooksTest extends V2PlayerIntegrationSpec {

  val mockOrgAndOptsForSpecs = mockOrgAndOpts(AuthMode.AccessToken)
  val vid = VersionedId(ObjectId.get, Some(0))
  val vidNoVersion = vid.copy(version = None)

  private class scope(
    val transformResult: JsValue = Json.obj(),
    val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOptsForSpecs)) extends Scope {

    lazy val itemService = mock[ItemService]
    lazy val playS3 = {
      val m = mock[S3Service]
      m.download(any[String], any[String], any[Option[Headers]]) returns Results.Ok("ok")
      m.delete(any[String], any[String]) returns {
        val r = mock[DeleteResponse]
        r.success returns true
        r
      }

      m.s3ObjectAndData(any[String], any[Item => String])(any[RequestHeader => Either[SimpleResult, Item]]) returns {
        val out: BodyParser[Future[(S3Object, Item)]] = BodyParser.apply { rh =>
          val o: Iteratee[Array[Byte], Either[SimpleResult, Future[(S3Object, Item)]]] = new Iteratee[Array[Byte], Either[SimpleResult, Future[(S3Object, Item)]]] {
            override def fold[B](folder: (Step[Array[Byte], Either[SimpleResult, Future[(S3Object, Item)]]]) => Future[B])(implicit ec: ExecutionContext): Future[B] = {
              val s3o = mock[S3Object]
              s3o.getKey returns "some/path.png"
              val item = Item(collectionId = ObjectId.get.toString)
              folder(Step.Done(Right(Future((s3o, item))), Input.Empty))
            }
          }
          o
        }
        out
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

  "load" should {
    "load the item" in new scope {
      hooks.load("itemId").map(_.isRight) must equalTo(true).await
    }

    "load calls transform" in new scope(transformResult = Json.obj("transformed" -> true)) {
      hooks.load("itemId").map(_.right.get) must equalTo(transformResult).await
    }

    "return the itemAuth.loadForWrite error" in new scope() {
      val err = TestError("itemAuth.loadForWrite")
      itemAuth.loadForWrite(any[String])(any[OrgAndOpts]) returns Failure(err)
      hooks.load("itemId").map(_.left.get) must equalTo((err.statusCode, err.message)).await
    }

    "return the org error" in new scope(orgAndOpts = Failure(TestError("org and opts"))) {
      val err = TestError("org and opts")
      hooks.load("itemId").map(_.left.get) must equalTo((err.statusCode, err.message)).await
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

    "return the org error" in new scope(orgAndOpts = Failure(TestError("org and opts"))) {
      val result = hooks.loadFile(vid.toString, "path")(fakeRequest)
      status(Future(result)) === orgAndOpts.toEither.left.get.statusCode
      contentAsString(Future(result)) === orgAndOpts.toEither.left.get.message
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
  }

  "upload" should {
    "call itemService.addFileToPlayerDefinition" in new scope {

      val mockItem = Item(collectionId = ObjectId.get.toString)
      val mockKey = "some/mock-key.png"

      playS3.s3ObjectAndData(any[String], any[Item => String])(any[RequestHeader => Either[SimpleResult, Item]]) returns {
        BodyParser.apply { rh =>
          new Iteratee[Array[Byte], Either[SimpleResult, Future[(S3Object, Item)]]] {
            override def fold[B](folder: (Step[Array[Byte], Either[SimpleResult, Future[(S3Object, Item)]]]) => Future[B])(implicit ec: ExecutionContext): Future[B] = {
              val s3o = mock[S3Object]
              s3o.getKey returns mockKey
              folder(Step.Done(Right(Future((s3o, mockItem))), Input.Empty))
            }
          }
        }
      }

      val bp: BodyParser[Future[UploadResult]] = hooks.upload(vid.toString, mockKey)((rh: RequestHeader) => None)
      val i: Iteratee[Array[Byte], Either[SimpleResult, Future[UploadResult]]] = bp(fakeRequest)
      val result: Either[SimpleResult, Future[UploadResult]] = await(i.run)
      val uploadResult = await(result.right.get)
      val file = StoredFile(mockKey, BaseFile.getContentType(mockKey), false, grizzled.file.util.basename(mockKey))
      there was one(itemService).addFileToPlayerDefinition(mockItem, file)
    }
  }
}
