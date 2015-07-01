package org.corespring.player.v1.controllers

import org.corespring.common.encryption.{ AESCrypto, Crypto }
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.encryption._
import org.corespring.player.accessControl.models.RenderOptions
import play.api.libs.json._
import scalaz.Scalaz._
import scalaz._

class Encrypter(encrypter: Crypto) extends BaseApi {

  private implicit object Writes extends Writes[EncryptionSuccess] {
    def writes(er: EncryptionSuccess): JsValue = JsObject {
      Seq(
        "clientId" -> JsString(er.clientId),
        "options" -> JsString(er.data),
        "request" -> JsString(er.requested.getOrElse("")))
    }
  }

  def encryptOptions = ApiAction {
    request =>
      val apiClientEncrypter = new ApiClientEncrypter(encrypter)
      val result: Validation[String, EncryptionResult] = for {
        json <- request.body.asJson.toSuccess("No json in the request body")
        validJson <- if (validJson(json)) Success(json) else Failure("Not valid json")
        encryptionResult <- apiClientEncrypter.encryptByOrg(request.ctx.organization, validJson.toString())
          .toSuccess("No encryption created")
      } yield {
        encryptionResult
      }

      result match {
        case Success(r) => {
          r match {
            case s: EncryptionSuccess => Ok(Json.toJson(s))
            case f: EncryptionFailure => {
              logger.error("Failed encryption: " + f.e.getMessage)
              BadRequest(
                Json.toJson(
                  JsObject(
                    Seq(
                      "error" -> JsString("There was an error encrypting your options")))))
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

