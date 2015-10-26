package org.corespring.v2.player.hooks

import java.io.ByteArrayInputStream

import org.corespring.container.client.hooks.{ Binary, CreateBinaryMaterial }
import org.corespring.models.item.resource.{ Resource, StoredFileDataStream }
import org.corespring.services.item.SupportingMaterialsService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.integration.hooks.beFutureErrorCodeMessage
import org.corespring.v2.player.{ V2PlayerExecutionContext, V2PlayerIntegrationSpec }
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

class SupportingMaterialHooksTest extends V2PlayerIntegrationSpec with NoTimeConversions with Mockito with MockFactory {

  val orgAndOpts = Success(mockOrgAndOpts(AuthMode.AccessToken))

  val resource = Resource(name = "name", files = Seq.empty)

  def testError(key: String = "test-error") = generalError(key)

  val createBinary = CreateBinaryMaterial(
    "name",
    "type",
    Binary("image.png", "image/png", Array.empty))

  trait scope
    extends Scope with StubJsonFormatting {

    lazy val idResult: Validation[V2Error, String] = Success("id")

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

    def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOptsResult

    val hooks = new SupportingMaterialHooks[String](mockAuth,
      getOrgAndOptions,
      jsonFormatting,
      V2PlayerExecutionContext(ExecutionContext.global)) {
      override def parseId(id: String, identity: OrgAndOpts): Validation[V2Error, String] = parseIdResult

      def parseIdResult: Validation[V2Error, String] = idResult

      override def service: SupportingMaterialsService[String] = mockService
    }

  }

  implicit val req = FakeRequest("", "")

  "writeForResource" should {

    trait writeScope extends scope {

      def idToValidationResult: Validation[String, Resource] = Success(resource)

      def testWriteForResource = hooks.writeForResource("id", FakeRequest("", "")) { vid =>
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
      override lazy val idResult = Failure(e)
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
      val result = waitFor(hooks.deleteAsset("id", "name", "file"))
      there was one(mockService).removeFile("id", "name", "file")
    }
  }

  "delete" should {
    "call service.delete" in new scope {
      val result = waitFor(hooks.delete("id", "name"))
      there was one(mockService).delete("id", "name")
    }
  }

  "getAsset" should {
    "call service.getFile" in new scope {
      val result = waitFor(hooks.getAsset("id", "name", "file"))
      there was one(mockService).getFile("id", "name", "file")
    }

    "return a FileDataStream" in new scope {
      val result = waitFor(hooks.getAsset("id", "name", "file"))
      result match {
        case Right(fds) => success
        case Left(_) => ko("expected a FileDataStream")
      }
    }
  }

  "updateContent" should {
    "call service.updateFileContent" in new scope {
      val result = waitFor(hooks.updateContent("id", "name", "file", "content"))
      there was one(mockService).updateFileContent("id", "name", "file", "content")
    }
  }
}
