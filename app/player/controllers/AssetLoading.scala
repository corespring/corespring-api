package player.controllers

import play.api.mvc.{Action, Controller}
import encryption.Decrypt

class AssetLoading(decrypter:Decrypt) extends Controller {

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
      Ok("alert('welcome to the item-player!')").as("text/javascript")
      //val options = decrypt(encryptedOptions, apiClientId)
      //play.api.libs.json.Json.parse(options)
    }

  }


  private def decrypt(encrypted:Option[String], apiClientId:Option[String]) : String = {
    decrypter.decrypt(encrypted.get, apiClientId.get)
  }
}

object NullDecrypt extends Decrypt{
  def decrypt(s:String,key:String) : String = s
}

object AssetLoading extends AssetLoading(NullDecrypt)