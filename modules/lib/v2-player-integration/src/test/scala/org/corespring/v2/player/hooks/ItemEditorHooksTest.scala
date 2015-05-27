package org.corespring.v2.player.hooks

import java.util.concurrent.TimeUnit

import com.amazonaws.services.s3.model.S3Object
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.container.client.hooks.UploadResult
import org.corespring.drafts.item.S3Paths
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ BaseFile, StoredFile }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.{ cantParseItemId, generalError }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.Play
import play.api.libs.iteratee.{ Input, Step, Iteratee }
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc._
import play.api.test.{ PlaySpecification, FakeRequest }

import scala.concurrent.{ ExecutionContext, Future, Await }
import scala.concurrent.duration.Duration
import scalaz.{ Failure, Success, Validation }

class ItemEditorHooksTest extends Specification with Mockito with MockFactory with PlaySpecification {

  PlaySingleton.start()

  val mockOrgAndOptsForSpecs = mockOrgAndOpts(AuthMode.AccessToken)
  val vid = VersionedId(ObjectId.get, Some(0))

  def TestError(msg: String) = generalError(msg)

  class scope(
    val transformResult: JsValue = Json.obj(),
    val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOptsForSpecs)) extends Scope with ItemEditorHooks {

    lazy val mockItemService = mock[ItemService]
    lazy val mockS3Service = {
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
              val item = Item()
              folder(Step.Done(Right(Future((s3o, item))), Input.Empty))
            }
          }
          o
        }
        out
      }
      m
    }
    lazy val mockItemAuth = {
      val m = mock[ItemAuth[OrgAndOpts]]
      m.loadForWrite(any[String])(any[OrgAndOpts]) returns Success(Item())
      m.canWrite(any[String])(any[OrgAndOpts]) returns Success(true)
      m
    }

    override def transform: (Item) => JsValue = (i) => transformResult

    override def itemService: ItemService = mockItemService

    override def playS3: S3Service = mockS3Service

    override def bucket: String = "bucket"

    override def itemAuth: ItemAuth[OrgAndOpts] = mockItemAuth

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

    def waitFor[A](f: Future[A]) = Await.result(f, Duration(1, TimeUnit.SECONDS))

    implicit val r = FakeRequest("", "")

  }

  "load" should {
    "load the item" in new scope {
      val either = waitFor(load("itemId"))
      either match {
        case Left(_) => failure("should have been successful")
        case Right(json) => success
      }
    }

    "load calls transform" in new scope(transformResult = Json.obj("transformed" -> true)) {
      val either = waitFor(load("itemId"))
      either.right.get === transformResult
    }

    "return the itemAuth.loadForWrite error" in new scope() {
      val err = TestError("itemAuth.loadForWrite")
      mockItemAuth.loadForWrite(any[String])(any[OrgAndOpts]) returns Failure(err)
      val either = waitFor(load("itemId"))
      either match {
        case Left((code, msg)) => {
          code === err.statusCode
          msg === err.message
        }
        case Right(json) => failure("should have failed")
      }
    }

    "return the org error" in new scope(orgAndOpts = Failure(TestError("org and opts"))) {
      val either = waitFor(load("itemId"))
      either match {
        case Left((code, msg)) => {
          val err = orgAndOpts.toEither.left.get
          code === err.statusCode
          msg === err.message
        }
        case Right(json) => failure("should have failed")
      }
    }
  }

  "loadFile" should {
    "call s3 download" in new scope() {
      val result = loadFile(vid.toString, "path")(r)
      there was one(mockS3Service).download("bucket", S3Paths.itemFile(vid, "path"), null)
    }

    "return cantParseItemId error" in new scope {
      val result = loadFile("bad id", "path")(r)
      val err = cantParseItemId("bad id")
      status(Future(result)) === err.statusCode
      contentAsString(Future(result)) === err.message
    }

    "return the org error" in new scope(orgAndOpts = Failure(TestError("org and opts"))) {
      val result = loadFile(vid.toString, "path")(r)
      status(Future(result)) === orgAndOpts.toEither.left.get.statusCode
      contentAsString(Future(result)) === orgAndOpts.toEither.left.get.message
    }
  }

  "deleteFile" should {
    "call s3.delete" in new scope {
      val result = waitFor(deleteFile(vid.toString, "path"))
      there was one(mockS3Service).delete("bucket", S3Paths.itemFile(vid, "path"))
    }

    "returns the s3 DeleteResponse message" in new scope {
      mockS3Service.delete(any[String], any[String]) returns {
        val r = mock[DeleteResponse]
        r.success returns false
        r.msg returns "s3 error"
        r
      }

      val result = waitFor(deleteFile(vid.toString, "path"))
      result === Some(BAD_REQUEST, "s3 error")
    }
  }

  "upload" should {
    "call itemService.addFileToPlayerDefinition" in new scope {

      val mockItem = Item()
      val mockKey = "some/mock-key.png"

      mockS3Service.s3ObjectAndData(any[String], any[Item => String])(any[RequestHeader => Either[SimpleResult, Item]]) returns {
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

      val bp: BodyParser[Future[UploadResult]] = upload(vid.toString, mockKey)((rh: RequestHeader) => None)
      val i: Iteratee[Array[Byte], Either[SimpleResult, Future[UploadResult]]] = bp(r)
      val result: Either[SimpleResult, Future[UploadResult]] = waitFor(i.run)
      val uploadResult = waitFor(result.right.get)
      println(uploadResult)
      val file = StoredFile(mockKey, BaseFile.getContentType(mockKey), false, grizzled.file.util.basename(mockKey))
      there was one(mockItemService).addFileToPlayerDefinition(mockItem, file)
    }
  }
}
