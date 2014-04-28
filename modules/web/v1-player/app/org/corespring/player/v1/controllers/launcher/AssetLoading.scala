package org.corespring.player.v1.controllers.launcher

import org.corespring.common.encryption.{ AESCrypto, Crypto }
import org.corespring.common.log.PackageLogging
import org.corespring.common.url.BaseUrl
import org.corespring.common.utils.string
import org.corespring.platform.core.controllers.AssetResource
import org.corespring.platform.core.services.item.{ ItemServiceWired, ItemService }
import org.corespring.player.accessControl.models.RenderOptions
import play.api.Play
import play.api.mvc._
import org.apache.commons.lang3.StringEscapeUtils

class AssetLoading(crypto: Crypto, playerTemplate: => String, val itemService: ItemService, errorHandler: String => Result)
  extends Controller
  with AssetResource
  with PlayerCookieWriter {

  object ErrorMessages {

    def apiClientNotFound(id: String) = "Can't find api client with id: " + id
    def queryParamNotFound(key: String, queryString: Map[String, Seq[String]]) = "Can't find parameter '" + key + "' on query string: " + queryString
    val InvalidObjectId = "Invalid ObjectId"
    def badJsonString(s: String, e: Throwable) = escape("Can't parse string into json: " + s)
    def cantConvertJsonToRenderOptions(s: String) = escape("Can't convert json to options: " + s)

    private def escape(s: String): String = StringEscapeUtils.escapeEcmaScript(s)

  }

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
    val preppedJs = AssetLoadingDefaults.createJsFromTemplate(playerTemplate, Map("mode" -> "preview", "baseUrl" -> getBaseUrl(request)))
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
              val preppedJs = AssetLoadingDefaults.createJsFromTemplate(template, tokenFn(options, request))
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



object AssetLoadingDefaults extends PackageLogging {

  def createJsFromTemplate(template: String, tokens: Map[String, String]): String = string.interpolate(template, string.replaceKey(tokens), string.DollarRegex)

  object Templates {

    import play.api.Play.current

    def player = load("public/js/corespring/corespring-player.js")
    def errorPlayer = load("public/js/corespring/corespring-error-player.js")

    private def load(p: String): String = {
      Play.resource(p).map { url =>
        val stream = url.openStream()
        io.Source.fromInputStream(stream).getLines.mkString("\n")
      }.getOrElse {
        logger.warn(s"Can't find resource: $p")
        ""
      }
    }
  }

  object ErrorHandler {

    import play.api.mvc.Results.Ok

    def handleError(msg: String): Result = {
      val out = AssetLoadingDefaults.createJsFromTemplate(Templates.errorPlayer, Map("playerError" -> msg))
      Ok(out).as("text/javascript")
    }
  }
}

class AssetLoadingMain extends AssetLoading(AESCrypto, AssetLoadingDefaults.Templates.player, ItemServiceWired, AssetLoadingDefaults.ErrorHandler.handleError)

object AssetLoading extends AssetLoadingMain


