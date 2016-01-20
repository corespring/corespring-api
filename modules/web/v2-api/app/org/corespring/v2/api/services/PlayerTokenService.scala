package org.corespring.v2.api.services

import org.corespring.encryption.apiClient.{ ApiClientEncryptionService, EncryptionFailure, EncryptionResult, EncryptionSuccess }
import org.corespring.models.auth.ApiClient
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.{ encryptionFailed, missingRequiredField }
import org.corespring.v2.errors.{ Field, V2Error }
import play.api.Logger
import play.api.libs.json.{ JsError, JsSuccess, JsValue, Json }

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

case class CreateTokenResult(apiClient: String, token: String, settings: JsValue)

class PlayerTokenService(service: ApiClientEncryptionService) {

  private lazy val logger = Logger(classOf[PlayerTokenService])

  def createToken(apiClient: ApiClient, json: JsValue): Validation[V2Error, CreateTokenResult] = for {
    accessSettings <- toAccessSettings(json)
    encryptionResult <- service.encrypt(apiClient, Json.stringify(Json.toJson(accessSettings)))
      .toSuccess(encryptionFailed(s"orgId: ${apiClient.orgId} - Unknown error trying to encrypt"))
    clientIdAndToken <- encryptionToValidation(encryptionResult)
  } yield {
    CreateTokenResult(clientIdAndToken._1, clientIdAndToken._2, Json.toJson(accessSettings))
  }

  private def encryptionToValidation(er: EncryptionResult): Validation[V2Error, (String, String)] = {
    er match {
      case EncryptionSuccess(clientId, encrypted, requested) => {
        logger.trace(s"function=createPlayerToken clientId=$clientId request=$requested")
        Success(clientId -> encrypted)
      }
      case EncryptionFailure(msg, e) => {
        logger.trace(s"function=createPlayerToken - encryption failed")
        Failure(encryptionFailed(msg))
      }
    }
  }

  private def toAccessSettings(json: JsValue): Validation[V2Error, PlayerAccessSettings] = PlayerAccessSettings.permissiveRead(json) match {
    case JsError(_) => Failure(missingRequiredField(Field("expires", "number")))
    case JsSuccess(o, _) => Success(o)
  }
}
