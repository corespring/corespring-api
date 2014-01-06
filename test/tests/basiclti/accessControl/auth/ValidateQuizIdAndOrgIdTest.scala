package tests.basiclti.accessControl.auth

import basiclti.accessControl.auth.ValidateQuizIdAndOrgId
import basiclti.accessControl.auth.cookies.LtiCookieKeys
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import play.api.mvc.Results._
import play.api.mvc.{SimpleResult, Result, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.corespring.test.PlaySingleton
import org.corespring.player.accessControl.cookies.PlayerCookieKeys
import scala.concurrent.{ExecutionContext, Future}

object MockImpl extends ValidateQuizIdAndOrgId[FakeRequest[AnyContent]] {
  def makeRequest(orgId: ObjectId, r: Request[AnyContent]): Option[FakeRequest[AnyContent]] = Some(r.asInstanceOf[FakeRequest[AnyContent]])
}

class ValidateQuizIdAndOrgIdTest extends Specification {

  PlaySingleton.start()

  "validate quiz id and org id" should {

    def run(keys: (String, String)*): Future[SimpleResult] = {

      import ExecutionContext.Implicits.global

      def mockQuery(quizId: String, orgId: String): Boolean = true
      def mockBlock(request: FakeRequest[AnyContent]): Future[SimpleResult] = Future(Ok("hello"))
      val q: MockImpl.OrgIdAndQuizIdAreValid = mockQuery
      MockImpl.ValidatedAction(q)(mockBlock)(FakeRequest().withSession(keys: _*))
    }

    def oid(): String = new ObjectId().toString

    "fail for nothing" in status(run()) === UNAUTHORIZED
    "fail for no quiz id" in status(run(PlayerCookieKeys.ORG_ID -> oid)) === UNAUTHORIZED
    "fail for no org id" in status(run(LtiCookieKeys.QUIZ_ID -> oid)) === UNAUTHORIZED
    "ok for quiz id and org id" in status(run(LtiCookieKeys.QUIZ_ID -> oid, PlayerCookieKeys.ORG_ID -> oid)) === OK

  }
}
