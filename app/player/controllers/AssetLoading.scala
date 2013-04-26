package player.controllers

import common.controllers.AssetResource
import common.controllers.utils.BaseUrl
import common.seed.StringUtils
import models.auth.ApiClient
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Logger, Play}
import player.accessControl.cookies.PlayerCookieWriter
import player.accessControl.models.RenderOptions
import scala.Some
import common.encryption.{Crypto, AESCrypto}

class AssetLoading(crypto: Crypto, playerTemplate: => String) extends Controller with AssetResource with PlayerCookieWriter {

  def itemProfileJavascript = renderJavascript(playerTemplate, {
    (ro: Option[RenderOptions], req: Request[AnyContent]) =>
      createJsTokens(ro, req) + ("mode" -> "preview")
  })

  def itemPlayerJavascript = renderJavascript(playerTemplate, {
    (ro: Option[RenderOptions], req: Request[AnyContent]) =>
      val mode = ro.map {
        _.mode
      }.getOrElse("preview")
      createJsTokens(ro, req) + ("mode" -> mode)
  })

  def getDataFileForAssessment(assessmentId: String, itemId: String, filename: String) = getDataFile(itemId, filename)


  /** Serve the player js
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
      withApiClient {
        implicit client =>
          withOptions {
            options =>
              val preppedJs = createJsFromTemplate(template, tokenFn(options, request))
              val newSession = sumSession(request.session, client.map(c => playerCookies(c.orgId)).getOrElse(Seq()): _*)
              Ok(preppedJs)
                .as("text/javascript")
                .withSession(newSession)
          }
      }
  }

  private def createJsTokens(o: Option[RenderOptions], r: Request[AnyContent]): Map[String, String] = Map("baseUrl" -> (BaseUrl(r) + "/player"))


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

  private def withOptions(block: Option[RenderOptions] => Result)(implicit request: Request[AnyContent], client: Option[ApiClient]) = client match {
    case Some(c) => {
      val result = for {
        o <- request.queryString.get("options").map(_.mkString)
        ro <- decryptOptions(o, c)
      } yield {
        block(Some(ro))
      }
      result.getOrElse(block(None))
    }
    case _ => block(None)
  }

  private def withApiClient(block: Option[ApiClient] => Result)(implicit request: Request[AnyContent]): Result = {
    val id = request.queryString.get("apiClientId").map(_.mkString)
    val maybeClient: Option[ApiClient] = for {
      i <- id
      client <- ApiClient.findByKey(i)
    } yield client
    block(maybeClient)
  }


  private def createJsFromTemplate(template: String, tokens: Map[String, String]): String = StringUtils.interpolate(template, StringUtils.replaceKey(tokens), StringUtils.DollarRegex)

}

object DefaultTemplate {

  import play.api.Play.current

  def player = io.Source.fromFile(Play.getFile("public/js/corespring/corespring-player.js")).getLines().mkString("\n")
}

object AssetLoading extends AssetLoading(AESCrypto, DefaultTemplate.player)

//object AssetLoading extends AssetLoading(MockUrlEncodeEncrypter, DefaultTemplate.template)
