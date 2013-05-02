package player.controllers

import common.encryption.EncryptionResult
import common.encryption.{Crypto, AESCrypto, OrgEncrypter}
import controllers.auth.BaseApi
import play.api.libs.json._
import player.accessControl.models.RenderOptions
import scalaz._
import Scalaz._

class Encrypter(encrypter: Crypto) extends BaseApi {

  private implicit object Writes extends Writes[EncryptionResult] {
    def writes(er: EncryptionResult): JsValue = JsObject {
      Seq(
        "clientId" -> JsString(er.clientId),
        "options" -> JsString(er.data)
      )
    }
  }

  def encryptOptions = ApiAction {
    request =>
      val orgEncrypter = new OrgEncrypter(request.ctx.organization, encrypter)
      val result : Validation[String,EncryptionResult] = for {
        json <- request.body.asJson.toSuccess("No json in the request body")
        validJson <- if (validJson(json)) Success(json) else Failure("Not valid json")
        encryptionResult <- orgEncrypter.encrypt(validJson.toString()).toSuccess("No encryption created")
      } yield {
        encryptionResult
      }

      result match {
        case Success(r) => Ok(Json.toJson(r))
        case Failure(e) => BadRequest("Bad Request: " + e)
      }
  }


  private def validJson(json: JsValue): Boolean = try {
    Json.fromJson[RenderOptions](json)
    true
  } catch {
    case t: Throwable => false
  }


}

object Encrypter extends Encrypter(AESCrypto)


