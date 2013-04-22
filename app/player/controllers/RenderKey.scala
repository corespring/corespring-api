package player.controllers

import play.api.mvc.Controller
import encryption.{AESCrypto, Crypto}
import controllers.auth.{RenderOptions, BaseApi}
import play.api.libs.json.{JsString, JsObject, Json}
import models.auth.ApiClient

class RenderKey(encrypter:Crypto) extends BaseApi{

  def encrypt = ApiAction{ request =>
    request.body.asJson match {
      case Some(jsoptions) => {
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
      }
      case None => BadRequest(JsObject(Seq("message" -> JsString("your request must contain json properties containing the constraints of the key"))))
    }
  }
}


object RenderKey extends RenderKey(AESCrypto)


