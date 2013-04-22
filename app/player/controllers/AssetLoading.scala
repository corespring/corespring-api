package player.controllers

import common.seed.StringUtils
import encryption.{MockUrlEncodeEncrypter, AESCrypto, Crypto}
import play.api.Play
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.{Action, Controller}
import models.auth.ApiClient
import org.bson.types.ObjectId
import common.controllers.AssetResource
import common.controllers.utils.BaseUrl
import controllers.auth.RenderOptions
import api.ApiError


class AssetLoading(crypto:Crypto, playerTemplate: => String ) extends Controller with AssetResource {

  /** Serve the item player js
    * We require 2 parameters to be passed in with this url:
    * orgId and an encrypted options string
    *
    * We decrypt the options and set them as a session cookie so that related calls to @player.controllers.Session
    * will be authenticated.
    * @return
    */
  def itemPlayerJavascript = Action{ request =>

    val apiClientId : Option[String] = request.queryString.get("apiClientId").map(_.mkString)
    val encryptedOptions : Option[String] = request.queryString.get("options").map(_.mkString)
    val itemId:Option[String] = request.queryString.get("itemId").map(_.mkString)
    val sessionId:Option[String] = request.queryString.get("sessionId").map(_.mkString)
    val assessmentId:Option[String] = request.queryString.get("assessmentId").map(_.mkString)
    val role:Option[String] = request.queryString.get("role").map(_.mkString)
    val mode:Option[String] = request.queryString.get("mode").map(_.mkString)

    if( apiClientId.isEmpty ){
      Ok("alert('error: no apiClientId specified');").as("text/javascript")
    } else if( encryptedOptions.isEmpty){
      Ok("alert('error: no options specified');").as("text/javascript")
    }
    else {

      def decryptOptions(apiClient:ApiClient) : RenderOptions = {
        val options =  crypto.decrypt(encryptedOptions.get, apiClient.clientSecret)
        Json.fromJson[RenderOptions](Json.parse(options))
      }

      ApiClient.findByKey(apiClientId.get) match {
        case Some(client) => {
          val options = decryptOptions(client)
          options.overwriteOptions(RenderOptions(itemId,sessionId,assessmentId,role,options.expires,mode.getOrElse(options.mode))) match {
            case Right(finalOptions) =>
              val preppedJs = renderJs(BaseUrl(request) + "/player", finalOptions.mode)
              Ok(preppedJs)
                .as("text/javascript")
                .withSession("renderOptions" -> Json.toJson(finalOptions).toString, "orgId" -> client.orgId.toString)
            case Left(e) =>
              BadRequest(Json.toJson(ApiError.ItemPlayer(e.clientOutput)))
          }
        }
        case _ => BadRequest("can't find api client")
      }

    }
  }

  private def renderJs(baseUrl : String, mode:String) : String = {
    val tokens = Map( "baseUrl" -> baseUrl, "mode" -> mode)
    StringUtils.interpolate(playerTemplate, StringUtils.replaceKey(tokens), StringUtils.DollarRegex)
  }

  def getDataFileForAssessment(assessmentId:String, itemId:String, filename : String ) = getDataFile(itemId, filename)
}

object DefaultTemplate {

  import play.api.Play.current

  def template = io.Source.fromFile(Play.getFile("public/js/corespring/corespring-player.js")).getLines().mkString("\n")
}

object AssetLoading extends AssetLoading(AESCrypto, DefaultTemplate.template)
//object AssetLoading extends AssetLoading(MockUrlEncodeEncrypter, DefaultTemplate.template)
