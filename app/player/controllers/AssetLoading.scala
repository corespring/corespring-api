package player.controllers

import common.controllers.AssetResource
import common.controllers.utils.BaseUrl
import org.bson.types.ObjectId
import play.api.Play
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import scala.Some
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.common.utils.string
import org.corespring.common.encryption.{ Crypto, AESCrypto }
import org.corespring.player.accessControl.cookies.PlayerCookieWriter
import org.corespring.player.accessControl.models.RenderOptions
import org.corespring.platform.core.services.item.{ ItemServiceImpl, ItemService }
import org.corespring.player.accessControl.auth.AuthParams

class AssetLoading(playerTemplate: => String, val itemService: ItemService, errorHandler: String => Result) extends Controller with AssetResource with PlayerCookieWriter with AuthParams {

  def itemProfileJavascript = renderJavascript(playerTemplate, {
    (ro: Option[RenderOptions], req: Request[AnyContent]) =>
      createJsTokens(ro, req) + ("mode" -> "preview")
  })

  def itemPlayerJavascript = renderJavascript(playerTemplate, {
    (ro: Option[RenderOptions], req: Request[AnyContent]) =>
      val mode = ro.map {
        _.mode.toString
      }.getOrElse("preview")
      createJsTokens(ro, req) + ("mode" -> mode)
  })

  /**
   * Load the player js - but don't set any session cookies.
   * This is used for scenarios where the user is already authenticated and has an appropriate session cookie
   */
  def noSessionPlayerJavascript = Action { request =>
    val preppedJs = AssetLoading.createJsFromTemplate(playerTemplate, Map("mode" -> "preview", "baseUrl" -> getBaseUrl(request)))
    Ok(preppedJs)
      .as("text/javascript")
  }

  def getDataFileForAssessment(assessmentId: String, itemId: String, filename: String) = getDataFile(itemId, filename)

  /**
   * Serve the player js
   * There are 2 optional parameters to be passed in with this url:
   * orgId and an encrypted options string
   *
   * If these are present we decrypt the options and set them as a session cookie so that related calls to @player.controllers.Session
   * will be authenticated.
   *
   * If they are not present - no cookies are set
   * @return
   */
  private def renderJavascript(template: => String, tokenFn: ((Option[RenderOptions], Request[AnyContent]) => Map[String, String])) = Action {
    implicit request =>
      withApiClient(errorHandler) {
        implicit client =>
          withOptions(errorHandler) {
            options =>
              val preppedJs = AssetLoading.createJsFromTemplate(template, tokenFn(options, request))
              val newSession = sumSession(request.session, playerCookies(client.orgId, options): _*)
              Ok(preppedJs)
                .as("text/javascript")
                .withSession(newSession)
          }
      }
  }

  protected def getBaseUrl(r: Request[AnyContent]): String = BaseUrl(r) + "/player"

  private def createJsTokens(o: Option[RenderOptions], r: Request[AnyContent]): Map[String, String] =
  {
    Map("baseUrl" -> getBaseUrl(r) , "rawQueryString" -> r.rawQueryString)
  }

}



object AssetLoadingDefaults {

  object Templates {

    import play.api.Play.current

    def player = load("public/js/corespring/corespring-player.js")
    def errorPlayer = load("public/js/corespring/corespring-error-player.js")
    private def load(p: String): String = io.Source.fromFile(Play.getFile(p)).getLines().mkString("\n")
  }

  object ErrorHandler {

    import play.api.mvc.Results.Ok

    def handleError(msg: String): Result = {
      val out = AssetLoading.createJsFromTemplate(Templates.errorPlayer, Map("playerError" -> msg))
      Ok(out).as("text/javascript")
    }
  }
}

object AssetLoading extends AssetLoading(AssetLoadingDefaults.Templates.player, ItemServiceImpl, AssetLoadingDefaults.ErrorHandler.handleError) {

  def createJsFromTemplate(template: String, tokens: Map[String, String]): String = string.interpolate(template, string.replaceKey(tokens), string.DollarRegex)

}


