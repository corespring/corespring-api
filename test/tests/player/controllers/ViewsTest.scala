package tests.player.controllers

import org.corespring.platform.core.models.item.service.ItemServiceImpl
import org.specs2.execute.{Result => SpecsResult}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import player.accessControl.cookies.PlayerCookieKeys
import player.accessControl.models.RequestedAccess
import player.controllers.Views
import org.corespring.test.PlaySingleton

class ViewsTest extends Specification with Mockito {

  import TestIds._

  PlaySingleton.start()

  val views = new Views(new TestBuilder, ItemServiceImpl)

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
