package tests.basiclti.accessControl.auth

import basiclti.accessControl.auth.ValidateQuizIdAndOrgId
import basiclti.accessControl.auth.cookies.LtiCookieKeys
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import play.api.mvc.Results._
import play.api.mvc.{Result, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import player.accessControl.cookies.PlayerCookieKeys
import tests.PlaySingleton

object MockImpl extends ValidateQuizIdAndOrgId[FakeRequest[AnyContent]] {

  /** Build the request to be passed into the block */
  def makeRequest(orgId: ObjectId, r: Request[AnyContent]): Option[FakeRequest[AnyContent]] = Some(r.asInstanceOf[FakeRequest[AnyContent]])
}


class ValidateQuizIdAndOrgIdTest extends Specification {

  PlaySingleton.start()

  def mockQuery(quizId: String, orgId: String): Boolean = true

  def mockBlock(request: FakeRequest[AnyContent]): Result = Ok("hello")

  "validate quiz id and org id" should {

    def run(keys: (String, String)*): Result = {
      val q: MockImpl.OrgIdAndQuizIdAreValid = mockQuery
      MockImpl.ValidatedAction(q)(mockBlock)(FakeRequest().withSession(keys: _*))
    }
    def oid(): String = new ObjectId().toString

    "return unauthorized if no quiz id or org id in session" in status(run()) === UNAUTHORIZED
    "no quiz id" in status(run(PlayerCookieKeys.ORG_ID -> oid)) === UNAUTHORIZED
    "no org id" in status(run(LtiCookieKeys.QUIZ_ID -> oid)) === UNAUTHORIZED
    "quiz id and org id" in status(run(LtiCookieKeys.QUIZ_ID -> oid, PlayerCookieKeys.ORG_ID -> oid)) === OK

  }
}
