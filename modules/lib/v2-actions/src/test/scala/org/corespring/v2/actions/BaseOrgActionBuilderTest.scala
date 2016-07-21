package org.corespring.v2.actions

import org.corespring.v2.actions.V2Actions.GetOrgAndOpts
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.mvc.Results._
import play.api.mvc.{ AnyContent, Request, RequestHeader, SimpleResult }
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scalaz.{ Failure, Success, Validation }

class BaseOrgActionBuilderTest extends Specification with MockFactory with NoTimeConversions {

  class Builder(getOrgAndOpts: GetOrgAndOpts) extends BaseOrgActionBuilder[OrgRequest](
    V2ActionExecutionContext(ExecutionContext.global),
    getOrgAndOpts) {
    override def makeWrappedRequest[A](rh: Request[A], id: OrgAndOpts): Validation[V2Error, OrgRequest[A]] = Success(OrgRequest(rh, id))
  }

  trait scope extends Scope {

    lazy val req = FakeRequest()
    lazy val result: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts())
    def fn(rh: RequestHeader) = result
    lazy val builder = new Builder(rh => Future.successful(fn(rh)))
    def actionBlock(r: OrgRequest[AnyContent]): Future[SimpleResult] = Future.successful(Ok("success!"))
    def blockingActionBlock(r: OrgRequest[AnyContent]): SimpleResult = Await.result(actionBlock(r), 1.second)
  }

  "invoke" should {

    "return error json - async" in new scope {
      val err = generalError("test error")
      override lazy val result = Failure(err)
      val r = builder.async(actionBlock(_))(req)
      status(r) must_== err.statusCode
      contentAsJson(r) must_== err.json
    }

    "return error json - blocking" in new scope {
      val err = generalError("test error")
      override lazy val result = Failure(err)
      val r: Future[SimpleResult] = builder.apply(blockingActionBlock(_))(req)
      status(r) must_== err.statusCode
      contentAsJson(r) must_== err.json
    }

    "return actionBlock result - async" in new scope {
      val r = builder.async(actionBlock(_))(req)
      status(r) must_== OK
      contentAsString(r) must_== "success!"
    }

    "return actionBlock result - blocking" in new scope {
      val r = builder.apply(blockingActionBlock(_))(req)
      status(r) must_== OK
      contentAsString(r) must_== "success!"
    }
  }
}
