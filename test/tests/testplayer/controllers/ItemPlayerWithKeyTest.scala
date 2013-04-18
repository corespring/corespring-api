package tests.testplayer.controllers

import play.api.mvc.{AnyContentAsJson, Call}
import controllers.auth.RenderOptions
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.mvc.Call
import play.api.test.FakeHeaders
import play.api.mvc.AnyContentAsJson
import scala.Some
import tests.BaseTest


class ItemPlayerWithKeyTest extends BaseTest{
  /**
   * item is 50083ba9e4b071cb5ef79101
   * session is 502d0f823004deb7f4f53be7
   * assessment is 000000000000000000000002
   */
  val update:Call = testplayer.controllers.routes.ItemPlayerWithKey.renderOptions()
  val renderModeOptions = RenderOptions(None,Some("502d0f823004deb7f4f53be7"),None,None,0,"render")
  val fakeRequest = FakeRequest(update.method,tokenize(update.url),FakeHeaders(),AnyContentAsJson(Json.toJson(renderConstraints)))
  val Some(result) = routeAndCall(fakeRequest)
}
