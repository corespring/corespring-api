package tests.player.controllers

import common.encryption.NullCrypto
import org.specs2.mutable.Specification
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeHeaders}
import player.accessControl.cookies.PlayerCookieKeys
import player.controllers.AssetLoading
import tests.PlaySingleton
import player.accessControl.models.{RequestedAccess, RenderOptions}
import play.api.libs.json.Json

class AssetLoadingTest extends Specification {

  PlaySingleton.start()

  val mockTemplate = """${mode}"""
  val loader = new AssetLoading(NullCrypto, mockTemplate)
  "asset loading" should {
    "load the js and set the cookies " in {
      val options : RenderOptions = RenderOptions(expires = 0, mode = RequestedAccess.Mode.Preview)
      val optionsString = Json.toJson(options).toString()
      val result = loader.itemPlayerJavascript(FakeRequest("", "blah?apiClientId=502d46ce0364068384f217a3&options="+optionsString, FakeHeaders(), AnyContentAsEmpty))
      status(result) === OK
      session(result).get(PlayerCookieKeys.ORG_ID) === Some("51114b307fc1eaa866444648")
      session(result).get(PlayerCookieKeys.RENDER_OPTIONS) === Some(optionsString)
      contentAsString(result) === "preview"
    }

    "load the js and don't add any cookies" in {
      val result = loader.itemPlayerJavascript(FakeRequest("", "blah", FakeHeaders(), AnyContentAsEmpty))
      status(result) === OK
      session(result).get(PlayerCookieKeys.ORG_ID) === None
      session(result).get(PlayerCookieKeys.RENDER_OPTIONS) === None
      contentAsString(result) === "preview"
    }
  }
}
