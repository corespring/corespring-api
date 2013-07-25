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
import models.item.service.ItemServiceImpl
import org.specs2.execute.Result

class AssetLoadingTest extends Specification {

  PlaySingleton.start()

  val mockTemplate = """${mode}"""

  val apiClientId = "502d46ce0364068384f217a3"

  import play.api.mvc.Results.BadRequest

  val loader = new AssetLoading(NullCrypto, mockTemplate, ItemServiceImpl, (msg:String) => BadRequest(msg))
  "asset loading" should {
    "load the js and set the cookies " in {
      val options : RenderOptions = RenderOptions(expires = 0, mode = RequestedAccess.Mode.Preview)
      val optionsString = Json.toJson(options).toString()
      val result = loader.itemPlayerJavascript(FakeRequest("", "blah?apiClientId="+apiClientId+"&options="+optionsString, FakeHeaders(), AnyContentAsEmpty))
      status(result) === OK
      session(result).get(PlayerCookieKeys.ORG_ID) === Some("51114b307fc1eaa866444648")
      session(result).get(PlayerCookieKeys.RENDER_OPTIONS) === Some(optionsString)
      contentAsString(result) === "preview"
    }

    def assertErrors(url:String, optionsJson:Option[String] = None, expectedErrorMessage:String) : Result = {
      val finalUrl = url + optionsJson.map( "&options=" + _).getOrElse("")
      val result = loader.itemPlayerJavascript(FakeRequest("", finalUrl, FakeHeaders(), AnyContentAsEmpty))
      status(result) === BAD_REQUEST
      contentAsString(result) === expectedErrorMessage
    }

    "return errors" in {
      assertErrors("blah", None, AssetLoading.ErrorMessages.queryParamNotFound("apiClientId", Map()))
      assertErrors("blah?apiClientId=blah", None, AssetLoading.ErrorMessages.InvalidObjectId)
      assertErrors("blah?apiClientId=" + apiClientId, None, AssetLoading.ErrorMessages.queryParamNotFound("options", Map("apiClientId" -> Seq(apiClientId))))
      assertErrors("blah?apiClientId=" + apiClientId, Some("bad-json"), AssetLoading.ErrorMessages.badJsonString("bad-json", new Exception()))
      val badId = "000000000000000000000001"
      assertErrors("blah?apiClientId=" + badId, None, AssetLoading.ErrorMessages.apiClientNotFound(badId))
      assertErrors("blah?apiClientId=" + apiClientId, Some("{}"), AssetLoading.ErrorMessages.cantConvertJsonToRenderOptions("{}") )
    }
  }
}
