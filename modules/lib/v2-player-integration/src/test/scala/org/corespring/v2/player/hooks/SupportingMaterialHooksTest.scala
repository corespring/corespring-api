package org.corespring.v2.player.hooks

import java.io.ByteArrayInputStream

import org.corespring.container.client.hooks.{ Binary, CreateBinaryMaterial }
import org.corespring.platform.core.models.item.resource.{ Resource, StoredFileDataStream }
import org.corespring.platform.core.services.item.SupportingMaterialsService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.integration.hooks.beFutureErrorCodeMessage
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.duration._
import scala.concurrent.{ Future, Await, ExecutionContext }
import scalaz.{ Failure, Success, Validation }

class SupportingMaterialHooksTest extends Specification with NoTimeConversions with Mockito with MockFactory {

  val idResult = Success("id")
  val orgAndOpts = Success(mockOrgAndOpts(AuthMode.AccessToken))

  val resource = Resource(name = "name", files = Seq.empty)

  def testError(key: String = "test-error") = generalError(key)

  val createBinary = CreateBinaryMaterial(
    "name",
    "type",
    Binary("image.png", "image/png", Array.empty))

  trait scope extends Scope
    with SupportingMaterialHooks[String] {

    def parseIdResult: Validation[V2Error, String] = idResult

    def orgAndOptsResult: Validation[V2Error, OrgAndOpts] = orgAndOpts

    val mockAuth = {
      val m = mock[ItemAuth[OrgAndOpts]]
      m.canWrite(any[String])(any[OrgAndOpts]) returns Success(true)
      m
    }

    val mockService = {
      val m = mock[SupportingMaterialsService[String]]

      m.updateFileContent(any[String], any[String], any[String], any[String]) returns Success(resource)

      m.delete(any[String], any[String]) returns Success(Seq(resource))

      m.removeFile(any[String], any[String], any[String]) returns Success(resource)

      m.getFile(any[String], any[String], any[String], any[Option[String]]) returns Success(StoredFileDataStream(
        "file",
        new ByteArrayInputStream(Array()),
        0,
        "image/png",
        Map.empty))

      /**
       * Can't mock create at the moment because of a byname param.
       * m.create(any[String], any[Resource], any[Array[Byte]]) returns null
       */
      m
    }

    override def auth: ItemAuth[OrgAndOpts] = mockAuth

    override def parseId(id: String, identity: OrgAndOpts): Validation[V2Error, String] = parseIdResult

    override def service: SupportingMaterialsService[String] = mockService

    override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOptsResult
  }

  implicit val req = FakeRequest("", "")

  "writeForResource" should {

    trait writeScope extends scope {

      def idToValidationResult: Validation[String, Resource] = Success(resource)

      def testWriteForResource = writeForResource("id", FakeRequest("", "")) { vid =>
        idToValidationResult
      }
    }

    "return an orgAndOptsError" in new writeScope {
      val e = testError("org-and-opts")
      override lazy val orgAndOptsResult = Failure(e)
      testWriteForResource must beFutureErrorCodeMessage(e.statusCode, e.msg)
    }

    "return an parseId error" in new writeScope {
      val e = testError("id")
      override lazy val parseIdResult = Failure(e)
      testWriteForResource must beFutureErrorCodeMessage(e.statusCode, e.msg)
    }

    "return an auth.canWrite error" in new writeScope {
      val e = testError("can-write")
      mockAuth.canWrite(any[String])(any[OrgAndOpts]) returns Failure(e)
      testWriteForResource must beFutureErrorCodeMessage(e.statusCode, e.msg)
    }

    "returns the idToValidationError error" in new writeScope {
      override lazy val idToValidationResult = Failure("id-to-validation")
      testWriteForResource must beFutureErrorCodeMessage(testError().statusCode, "id-to-validation")
    }

  }

  "create" should {

    /**
     * We need to change the build so that specs2 supports by-name parameters.
     * we'll need to make sure that we have an appropriate specs2 version and that it's before mockito
     * in the classpath.
     * See: http://etorreborre.github.io/specs2/guide/SPECS2-3.0/org.specs2.guide.UseMockito.html
     */

    "call service.create" in pending
  }

  "addAsset" should {

    /**
     * See above - byname parameter support in specs2/mockito
     */
    "call service.addFile" in pending
  }

  def waitFor[A](fn: => Future[Either[(Int, String), A]]) = Await.result(fn, 1.second)

  "deleteAsset" should {

    "call service.removeFile" in new scope {
      val result = waitFor(deleteAsset("id", "name", "file"))
      there was one(mockService).removeFile("id", "name", "file")
    }
  }

  "delete" should {
    "call service.delete" in new scope {
      val result = waitFor(delete("id", "name"))
      there was one(mockService).delete("id", "name")
    }
  }

  "getAsset" should {
    "call service.getFile" in new scope {
      val result = waitFor(getAsset("id", "name", "file"))
      there was one(mockService).getFile("id", "name", "file")
    }

    "return a FileDataStream" in new scope {
      val result = waitFor(getAsset("id", "name", "file"))
      result match {
        case Right(fds) => success
        case Left(_) => ko("expected a FileDataStream")
      }
    }
  }

  "updateContent" should {
    "call service.updateFileContent" in new scope {
      val result = waitFor(updateContent("id", "name", "file", "content"))
      there was one(mockService).updateFileContent("id", "name", "file", "content")
    }
  }
}
