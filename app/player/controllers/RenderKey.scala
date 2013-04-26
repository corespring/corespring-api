package player.controllers

import play.api.mvc.{Result, Controller}
import encryption.{MockUrlEncodeEncrypter, AESCrypto, Crypto}
import controllers.auth.{BaseApi}
import play.api.libs.json.{JsString, JsObject, Json}
import models.auth.ApiClient
import models.Organization
import org.codehaus.jackson.JsonParseException
import api.ApiError
import player.accessControl.models.RenderOptions

class RenderKey(encrypter:Crypto) extends BaseApi{

  def encrypt = ApiAction{ request =>
    request.body.asJson match {
      case Some(jsoptions) => try{
        val options = Json.fromJson[RenderOptions](jsoptions)
        ApiClient.findOneByOrgId(request.ctx.organization) match {
          case Some(apiClient) => {
            val encryptedOptions = encrypter.encrypt(Json.toJson(options).toString(),apiClient.clientSecret)
            Ok(JsObject(Seq(
              "clientId" -> JsString(apiClient.clientId.toString),
              "options" -> JsString(encryptedOptions)
            )))
          }
          case None => BadRequest(JsObject(Seq("message" -> JsString("no api client found! this should never occur"))))
        }
      }catch{
        case e:JsonParseException => BadRequest(Json.toJson(ApiError.BadJson(Some("tried to parse RenderOptions"))))
      }
      case None => BadRequest(JsObject(Seq("message" -> JsString("your request must contain json properties containing the constraints of the key"))))
    }
  }
}


//object RenderKey extends RenderKey(MockUrlEncodeEncrypter)
object RenderKey extends RenderKey(AESCrypto)


