package player.controllers

import common.controllers.AssetResource
import common.controllers.utils.BaseUrl
import common.seed.StringUtils
import controllers.auth.RenderOptions
import encryption.{AESCrypto, Crypto}
import models.auth.ApiClient
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Logger, Play}
import scala.Some

class AssetLoading(crypto: Crypto, playerTemplate: => String) extends Controller with AssetResource {

  def itemProfileJavascript =  renderJavascript(playerTemplate, {
    (ro: RenderOptions, req: Request[AnyContent]) =>
      createJsTokens(ro, req) + ("mode" -> "preview")
  })

  def itemPlayerJavascript = renderJavascript(playerTemplate, {
    (ro: RenderOptions, req: Request[AnyContent]) =>
      createJsTokens(ro, req) + ("mode" -> ro.mode)
  })

  def getDataFileForAssessment(assessmentId: String, itemId: String, filename: String) = getDataFile(itemId, filename)


  /** Serve the some js
    * We require 2 parameters to be passed in with this url:
    * orgId and an encrypted options string
    *
    * We decrypt the options and set them as a session cookie so that related calls to @player.controllers.Session
    * will be authenticated.
    * @return
    */
  private def renderJavascript(template: => String, tokenFn: ((RenderOptions, Request[AnyContent]) => Map[String, String])) = Action {
    implicit request =>
      withApiClient {
        implicit client =>
          withOptions {
            options =>
              val preppedJs = createJsFromTemplate(template, tokenFn(options, request))
              Ok(preppedJs)
                .as("text/javascript")
                .withSession("renderOptions" -> Json.toJson(options).toString, "orgId" -> client.orgId.toString)
          }
      }
  }

  private def createJsTokens(o: RenderOptions, r: Request[AnyContent]): Map[String, String] = Map("baseUrl" -> (BaseUrl(r) + "/player"))


  private def decryptOptions(encryptedOptions: String, apiClient: ApiClient): Option[RenderOptions] = try {
    val options = crypto.decrypt(encryptedOptions, apiClient.clientSecret)
    Some(Json.fromJson[RenderOptions](Json.parse(options)))
  }
  catch {
    case e: Throwable => {
      Logger.warn("Error parsing options with apiClient id: " + apiClient.clientId)
      None
    }
  }

  private def withOptions(block: RenderOptions => Result)(implicit request: Request[AnyContent], client: ApiClient) = {
    for {
      o <- request.queryString.get("options").map(_.mkString)
      ro <- decryptOptions(o, client)
    } yield block(ro)
  }.getOrElse(Ok("alert('no render options found')"))

  private def withApiClient(block: ApiClient => Result)(implicit request: Request[AnyContent]) = {
    for {
      id <- request.queryString.get("apiClientId").map(_.mkString)
      client <- ApiClient.findByKey(id)
    } yield block(client)
  }.getOrElse(Ok("alert('no api client found')"))

  private def createJsFromTemplate(template: String, tokens: Map[String, String]): String =  StringUtils.interpolate(template, StringUtils.replaceKey(tokens), StringUtils.DollarRegex)

}

object DefaultTemplate {

  import play.api.Play.current

  def player = io.Source.fromFile(Play.getFile("public/js/corespring/corespring-player.js")).getLines().mkString("\n")
}

object AssetLoading extends AssetLoading(AESCrypto, DefaultTemplate.player)

//object AssetLoading extends AssetLoading(MockUrlEncodeEncrypter, DefaultTemplate.template)
