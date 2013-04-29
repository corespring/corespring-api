package tests.player.controllers

import controllers.auth.TokenizedRequestActionBuilder
import controllers.auth.requests.TokenizedRequest
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import player.accessControl.models.RequestedAccess
import player.controllers.Views
import tests.PlaySingleton
import player.accessControl.cookies.PlayerCookieKeys
import org.specs2.execute.{Success, Result => SpecsResult}

class ViewsTest extends Specification {

  val testId = new ObjectId("50b653a1e4b0ec03f29344b0")
  val testSessionId = new ObjectId("51116bc7a14f7b657a083c1d")
  val testQuizId = new ObjectId("000000000000000000000001")
  val testQuizItemId = new ObjectId("5153eee1aa2eefdc1b7a5570")

  PlaySingleton.start()

  class TestBuilder extends TokenizedRequestActionBuilder[RequestedAccess] {
    def ValidatedAction(access: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
      ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(access)(block)

    def ValidatedAction(p: BodyParser[AnyContent])(access: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>
        block(TokenizedRequest("test_token", request))
    }
  }

  val views = new Views(new TestBuilder)

  def assertCookie(a: Action[AnyContent], keyMode: (String, RequestedAccess.Mode.Mode)): SpecsResult = {
    val (key, mode) = keyMode
    val result = a(FakeRequest("", "", FakeHeaders(), AnyContentAsEmpty))
    status(result) === OK and session(result).get(key) === Some(mode.toString)
  }

  "views" should {
    "set the correct cookie" in {
      val key = PlayerCookieKeys.ACTIVE_MODE
      assertCookie(views.preview(testId), (key, RequestedAccess.Mode.Preview))
      assertCookie(views.administerItem(testId), (key, RequestedAccess.Mode.Administer))
      assertCookie(views.administerSession(testSessionId), (key, RequestedAccess.Mode.Administer))
      assertCookie(views.render(testSessionId), (key, RequestedAccess.Mode.Render))
      assertCookie(views.aggregate(testQuizId, testQuizItemId), (key, RequestedAccess.Mode.Aggregate))
      assertCookie(views.profile(testId, "tab"), (key, RequestedAccess.Mode.Preview))
    }
  }

}
