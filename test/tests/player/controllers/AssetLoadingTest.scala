package tests.player.controllers

import org.specs2.mutable.Specification
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeHeaders}
import player.controllers.AssetLoading
import play.api.libs.json.Json
import org.specs2.execute.Result
import org.corespring.test.PlaySingleton
import org.corespring.common.encryption.NullCrypto
import org.corespring.player.accessControl.cookies.PlayerCookieKeys
import org.corespring.player.accessControl.models.{RequestedAccess, RenderOptions}
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.apache.http.auth.params.AuthParams
import org.corespring.player.accessControl.auth.AuthParamErrorMessages

class AssetLoadingTest extends Specification {

  PlaySingleton.start()

  val mockTemplate = """${mode}"""

  val apiClientId = "502d46ce0364068384f217a3"

  import play.api.mvc.Results.BadRequest

  val loader = new AssetLoading(mockTemplate, ItemServiceImpl, (msg:String) => BadRequest(msg))
  loader.changeCrypto(NullCrypto)
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
      assertErrors("blah", None, AuthParamErrorMessages.queryParamNotFound("apiClientId", Map()))
      assertErrors("blah?apiClientId=blah", None, AuthParamErrorMessages.InvalidObjectId)
      assertErrors("blah?apiClientId=" + apiClientId, None, AuthParamErrorMessages.queryParamNotFound("options", Map("apiClientId" -> Seq(apiClientId))))
      assertErrors("blah?apiClientId=" + apiClientId, Some("bad-json"), AuthParamErrorMessages.badJsonString("bad-json", new Exception()))
      val badId = "000000000000000000000001"
      assertErrors("blah?apiClientId=" + badId, None, AuthParamErrorMessages.apiClientNotFound(badId))
      assertErrors("blah?apiClientId=" + apiClientId, Some("{}"), AuthParamErrorMessages.cantConvertJsonToRenderOptions("{}") )
    }
  }
}
