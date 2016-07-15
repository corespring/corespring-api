package org.corespring.v2.actions

import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }
import play.api.test.Helpers._
import play.api.mvc.Results._
import play.api.test.FakeRequest

class OrgActionBuilderTest extends Specification with MockFactory {

  trait scope extends Scope {
    lazy val result: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts())
    def fn(rh: RequestHeader) = result
    lazy val unauthorizedStatusCode: Option[Int] = None
    def getOrgAndOpts(rh: RequestHeader): Future[Validation[V2Error, OrgAndOpts]] = Future.successful(result)
    lazy val req = FakeRequest()
    lazy val builder = new OrgActionBuilder(
      V2ActionExecutionContext(ExecutionContext.global),
      getOrgAndOpts _,
      unauthorizedStatusCode)
  }

  "invoke" should {
    "return status code from error" in new scope {
      val error = generalError("test error")
      override lazy val result = Failure(error)
      status(builder.apply(rh => Ok("hi"))(req)) must_== error.statusCode
    }

    "return custom status code" in new scope {
      override lazy val unauthorizedStatusCode = Some(999)
      val error = generalError("test error")
      override lazy val result = Failure(error)
      status(builder.apply(rh => Ok("hi"))(req)) must_== 999
    }

    "return OK" in new scope {
      status(builder.apply(rh => Ok("hi"))(req)) must_== OK
    }
  }

}
