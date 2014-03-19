package org.corespring.lti.web.accessControl.auth

import org.bson.types.ObjectId
import org.corespring.lti.web.accessControl.cookies.LtiCookieKeys
import org.corespring.player.accessControl.cookies.PlayerCookieKeys
import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification
import play.api.mvc.Results._
import play.api.mvc.{SimpleResult, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.concurrent.{ExecutionContext, Future}

object MockImpl extends ValidateAssessmentIdAndOrgId[FakeRequest[AnyContent]] {
  def makeRequest(orgId: ObjectId, r: Request[AnyContent]): Option[FakeRequest[AnyContent]] = Some(r.asInstanceOf[FakeRequest[AnyContent]])
}

class ValidateAssessmentIdAndOrgIdTest extends Specification {

  PlaySingleton.start()

  "validate assessment id and org id" should {

    def run(keys: (String, String)*): Future[SimpleResult] = {

      import ExecutionContext.Implicits.global

      def mockQuery(assessmentId: String, orgId: String): Boolean = true
      def mockBlock(request: FakeRequest[AnyContent]): Future[SimpleResult] = Future(Ok("hello"))
      val q: MockImpl.OrgIdAndAssessmentIdAreValid = mockQuery
      MockImpl.ValidatedAction(q)(mockBlock)(FakeRequest().withSession(keys: _*))
    }

    def oid(): String = new ObjectId().toString

    "fail for nothing" in status(run()) === UNAUTHORIZED
    "fail for no assessment id" in status(run(PlayerCookieKeys.orgId -> oid)) === UNAUTHORIZED
    "fail for no org id" in status(run(LtiCookieKeys.ASSESSMENT_ID -> oid)) === UNAUTHORIZED
    "ok for assessment id and org id" in status(run(LtiCookieKeys.ASSESSMENT_ID -> oid, PlayerCookieKeys.orgId -> oid)) === OK

  }
}
