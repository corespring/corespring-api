package player.controllers

import play.api.mvc.{Action, Controller}
import encryption.{MockUrlEncodeEncrypter, Decrypt}
import play.api.libs.json.JsValue
import play.api.Play
import common.seed.StringUtils
import common.controllers.{AssetResource, ItemResources}

class AssetLoading(decrypter:Decrypt, playerTemplate: => String) extends Controller with AssetResource {

  /** Serve the item player js
    * We require 2 parameters to be passed in with this url:
    * orgId and an encrypted options string
    *
    * We decrypt the options and set them as a session cookie so that related calls to @player.controllers.Session
    * will be authenticated.
    * @return
    */
  def itemPlayer = Action{ request =>

    val apiClientId : Option[String] = request.queryString.get("apiClientId").map(_.mkString)
    val encryptedOptions : Option[String] = request.queryString.get("options").map(_.mkString)

    if( apiClientId.isEmpty ){
      Ok("alert('error: no apiClientId specified');").as("text/javascript")
    } else if( encryptedOptions.isEmpty){
      Ok("alert('error: no options specified');").as("text/javascript")
    }
    else {

      def options : JsValue = {
        val options =  decrypter.decrypt(encryptedOptions.get, apiClientId.get)
        play.api.libs.json.Json.parse(options)
      }

      val mode = (options \ "mode").asOpt[String]
      Ok(renderJs(mode)).as("text/javascript")
        .withSession("renderOptions" -> options.toString, "orgId" -> "TODO")

    }
  }

  private def renderJs(mode:Option[String]) : String = {
    val tokens = Map( "mode" -> mode.getOrElse("?"))
    StringUtils.interpolate(playerTemplate, StringUtils.replaceKey(tokens), StringUtils.DollarRegex)
  }
}

object DefaultTemplate {
  import play.api.Play.current
  def template = io.Source.fromFile(Play.getFile("public/js/corespring/corespring-player.js")).getLines().mkString("\n")
}

object AssetLoading extends AssetLoading(MockUrlEncodeEncrypter,  DefaultTemplate.template)