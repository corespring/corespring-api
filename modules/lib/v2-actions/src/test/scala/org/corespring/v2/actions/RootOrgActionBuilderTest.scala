package org.corespring.v2.actions

import org.corespring.models.appConfig.DefaultOrgs
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.notRootOrg
import org.corespring.v2.errors.V2Error
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Success, Validation }

class RootOrgActionBuilderTest extends Specification with MockFactory {

  trait scope extends Scope {
    lazy val req = FakeRequest()
    lazy val orgAndOpts = mockOrgAndOpts()
    lazy val result: Validation[V2Error, OrgAndOpts] = Success(orgAndOpts)
    def getOrgAndOpts(rh: RequestHeader) = Future.successful(result)
    lazy val defaultOrgs = DefaultOrgs(Nil, orgAndOpts.org.id)
    val v2ActionExecutionContext = V2ActionExecutionContext(ExecutionContext.global)
    val builder = new RootOrgActionBuilder(defaultOrgs, v2ActionExecutionContext, getOrgAndOpts(_))
  }

  "return action result for root org" in new scope {
    val r = builder.apply(r => Ok("success!"))(req)
    status(r) must_== OK
  }

  "return error if not the root org" in new scope {
    val otherOrg = mockOrgAndOpts()
    override lazy val result = Success(otherOrg)
    val r = builder.apply(r => Ok("success!"))(req)
    status(r) must_== UNAUTHORIZED
    contentAsJson(r) must_== notRootOrg(otherOrg.org).json
  }

}
