package player.controllers

import common.controllers.AssetResource
import common.controllers.utils.BaseUrl
import common.encryption.{Crypto, AESCrypto}
import common.utils.string
import models.auth.ApiClient
import models.item.service.{ItemServiceImpl, ItemService}
import org.bson.types.ObjectId
import play.api.Play
import play.api.libs.json.Json
import play.api.mvc._
import player.accessControl.cookies.PlayerCookieWriter
import player.accessControl.models.RenderOptions
import scala.Some
import scalaz.Scalaz._
import scalaz.{Failure, Success, Validation}


class AssetLoading(crypto: Crypto, playerTemplate: => String, val itemService : ItemService, errorHandler:String=>Result) extends Controller with AssetResource with PlayerCookieWriter {

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

  /** Load the player js - but don't set any session cookies.
   * This is used for scenarios where the user is already authenticated and has an appropriate session cookie
   */
  def noSessionPlayerJavascript = Action { request =>
    val preppedJs = AssetLoading.createJsFromTemplate(playerTemplate, Map("mode" -> "preview", "baseUrl" -> getBaseUrl(request)))
    Ok(preppedJs)
      .as("text/javascript")
  }

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
      withApiClient(errorHandler) {
        implicit client =>
          withOptions(errorHandler) {
            options =>
              val preppedJs = AssetLoading.createJsFromTemplate(template, tokenFn(options, request))
              val newSession = sumSession(request.session, playerCookies(client.orgId,options) : _*)
              Ok(preppedJs)
                .as("text/javascript")
                .withSession(newSession)
          }
      }
  }

  protected def getBaseUrl(r:Request[AnyContent]) : String = BaseUrl(r) + "/player"

  private def createJsTokens(o: Option[RenderOptions], r: Request[AnyContent]): Map[String, String] = Map("baseUrl" -> getBaseUrl(r))


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

  private def withOptions(errorBlock:String=>Result)(block: Option[RenderOptions] => Result)(implicit request: Request[AnyContent], client: ApiClient) = {

    import AssetLoading.ErrorMessages._

    val result : Validation[String,Result] = for{
      o <- request.queryString.get("options").map(_.mkString).toSuccess( queryParamNotFound("options", request.queryString))
      ro <- decryptOptions(o,client).toSuccess( DecryptOptions )
    } yield block(Some(ro))

      result match {
        case Success(r) => r
        case Failure(msg) => errorBlock(msg)
      }
  }


  private def withApiClient(errorBlock: String => Result)(block: ApiClient => Result)(implicit request: Request[AnyContent]): Result = {

    import AssetLoading.ErrorMessages._

    val result : Validation[String,Result] = for{
     id <- request.queryString.get("apiClientId").map(_.mkString).toSuccess( queryParamNotFound("apiClientId", request.queryString))
     validId <- if(ObjectId.isValid(id)) Success(id) else Failure(InvalidObjectId)
     client <- ApiClient.findByKey(id).toSuccess( apiClientNotFound(id) )
    } yield block(client)

    result match {
      case Success(r) => r
      case Failure(msg) => errorBlock(msg)
    }
  }
}

object AssetLoadingDefaults{

  object Templates {

    import play.api.Play.current

    def player = load("public/js/corespring/corespring-player.js")
    def errorPlayer = load("public/js/corespring/corespring-error-player.js")
    private def load(p:String) : String = io.Source.fromFile(Play.getFile(p)).getLines().mkString("\n")
  }

  object ErrorHandler {

    import play.api.mvc.Results.Ok

    def handleError(msg:String) : Result ={
      val out = AssetLoading.createJsFromTemplate(Templates.errorPlayer, Map("playerError" -> msg) )
      Ok(out).as("text/javascript")
    }
  }
}

object AssetLoading extends AssetLoading(AESCrypto, AssetLoadingDefaults.Templates.player, ItemServiceImpl, AssetLoadingDefaults.ErrorHandler.handleError) {

  def createJsFromTemplate(template: String, tokens: Map[String, String]): String = string.interpolate(template, string.replaceKey(tokens), string.DollarRegex)

  object ErrorMessages {

    def apiClientNotFound(id:String) = "Can't find api client with id: " + id
    def queryParamNotFound(key:String, queryString:Map[String,Seq[String]]) = "Can't find parameter '" + key + "' on query string: " + queryString
    val DecryptOptions = "Can't decrypt options"
    val InvalidObjectId = "Invalid ObjectId"
  }
}

