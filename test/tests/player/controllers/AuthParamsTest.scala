package tests.player.controllers

import org.specs2.mutable.Specification
import org.corespring.player.accessControl.auth.CheckSession
import org.corespring.player.accessControl.models.RequestedAccess.Mode
import org.corespring.player.accessControl.models.{RenderOptions, RequestedAccess}
import org.corespring.platform.core.models.error.InternalError
import org.corespring.common.encryption.NullCrypto
import org.corespring.test.PlaySingleton
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import scala.concurrent.{ExecutionContext, Future}
import play.api.http.Status.OK


/**
 * Verify that a request using query parameters will be authorized
 */
class AuthParamsTest extends Specification {

  import ExecutionContext.Implicits.global

  PlaySingleton.start()

  val apiClientId = "502d46ce0364068384f217a3"
  TestCheckSessionAccess.changeCrypto(NullCrypto)

  def testAction = TestCheckSessionAccess.ValidatedAction(RequestedAccess()) {
    request =>
      Future(play.api.mvc.Results.Ok)
  }

  "check session" should {
    "accept a request with no cookie but with render options in query params" in {
      val options : RenderOptions = RenderOptions(expires = 0, mode = RequestedAccess.Mode.Preview)
      val optionsString = Json.toJson(options).toString()

      val result = testAction(FakeRequest("", "blah?apiClientId="+apiClientId+"&options="+optionsString, FakeHeaders(), AnyContentAsEmpty))
      status(result) == OK
    }
  }

}

object TestCheckSessionAccess extends CheckSession {

  def grantAccess(activeMode: Option[Mode.Mode], a: RequestedAccess, o: RenderOptions): Either[InternalError, Boolean] = {
    if (o.expires == 0 && o.mode == RequestedAccess.Mode.Preview) {
      Right(true)
    } else {
      Left(InternalError("test failed"))
    }

  }
}
