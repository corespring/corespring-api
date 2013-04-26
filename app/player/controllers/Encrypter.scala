package player.controllers

import api.ApiError
import controllers.auth.BaseApi
import models.auth.ApiClient
import org.codehaus.jackson.JsonParseException
import play.api.libs.json.{JsString, JsObject, Json}
import player.accessControl.models.RenderOptions
import common.encryption.{Crypto, AESCrypto, EncryptionResult, OrgEncrypter}

class Encrypter(encrypter:Crypto) extends BaseApi{

  def encryptOptions = ApiAction{ request =>
    request.body.asJson match {
      case Some(jsoptions) => try{
        val options = Json.fromJson[RenderOptions](jsoptions)
        val orgEncrypter = new OrgEncrypter(request.ctx.organization, encrypter)
        orgEncrypter.encrypt(options.toString) match {
          case Some(EncryptionResult(clientId,data)) => {
            Ok(JsObject(Seq(
              "clientId" -> JsString(clientId),
              "options" -> JsString(data)
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

object Encrypter extends Encrypter(AESCrypto)


