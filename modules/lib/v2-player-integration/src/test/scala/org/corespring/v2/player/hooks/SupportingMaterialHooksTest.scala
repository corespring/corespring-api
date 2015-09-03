package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.{ Binary, CreateBinaryMaterial }
import org.corespring.platform.core.services.item.SupportingMaterialsService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.integration.hooks.beFutureErrorCodeMessage
import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.{ Future, ExecutionContext }
import scalaz.{ Failure, Success, Validation }

class SupportingMaterialHooksTest extends Specification with Mockito with MockFactory {

  val idResult = Success("id")
  val orgAndOptsResult = Success(mockOrgAndOpts(AuthMode.AccessToken))

  def testError = generalError("test-error")

  val createBinary = CreateBinaryMaterial(
    "name",
    "type",
    Binary("image.png", "image/png", Array.empty))

  class scope(
    parseIdResult: Validation[V2Error, String] = idResult,
    orgAndOptsResult: Validation[V2Error, OrgAndOpts] = orgAndOptsResult)
    extends Scope
    with SupportingMaterialHooks[String] {

    val mockAuth = {
      val m = mock[ItemAuth[OrgAndOpts]]
      m
    }

    val mockService = {
      val m = mock[SupportingMaterialsService[String]]
      m
    }

    override def auth: ItemAuth[OrgAndOpts] = mockAuth

    override def parseId(id: String, identity: OrgAndOpts): Validation[V2Error, String] = parseIdResult

    override def service: SupportingMaterialsService[String] = mockService

    override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOptsResult
  }

  implicit val req = FakeRequest("", "")

  "create" should {
    "call return orgAndOpts error" in new scope(orgAndOptsResult = Failure(testError)) {
      create("id", createBinary) must beFutureErrorCodeMessage(testError.statusCode, testError.msg)
    }
  }

  "deleteAsset" should {
    "work" in pending
  }

  "addAsset" should {
    "work" in pending
  }

  "delete" should {
    "work" in pending
  }

  "getAsset" should {
    "work" in pending
  }

  "updateContent" should {
    "work" in pending
  }
}
