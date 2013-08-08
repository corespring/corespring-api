package player.controllers

import common.encryption._
import controllers.auth.BaseApi
import play.api.libs.json._
import player.accessControl.models.RenderOptions
import scalaz.Scalaz._
import scalaz._
import org.corespring.common.encryption.{Crypto, AESCrypto}

class Encrypter(encrypter: Crypto) extends BaseApi {

  private implicit object Writes extends Writes[EncryptionSuccess] {
    def writes(er: EncryptionSuccess): JsValue = JsObject {
      Seq(
        "clientId" -> JsString(er.clientId),
        "options" -> JsString(er.data),
        "request" -> JsString(er.requested.getOrElse(""))
      )
    }
  }

  def encryptOptions = ApiAction {
    request =>
      val orgEncrypter = new OrgEncrypter(request.ctx.organization, encrypter)
      val result: Validation[String, EncryptionResult] = for {
        json <- request.body.asJson.toSuccess("No json in the request body")
        validJson <- if (validJson(json)) Success(json) else Failure("Not valid json")
        encryptionResult <- orgEncrypter.encrypt(validJson.toString()).toSuccess("No encryption created")
      } yield {
        encryptionResult
      }

      result match {
        case Success(r) => {
          r match {
            case s: EncryptionSuccess => Ok(Json.toJson(s))
            case f: EncryptionFailure => {
              Logger.error("Failed encryption: " + f.e.getMessage)
              BadRequest(
                Json.toJson(
                  JsObject(
                    Seq(
                      "error" -> JsString("There was an error encrypting your options")
                    )
                  )
                )
              )
            }
          }
        }
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


