package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.{ EncryptionFailure, EncryptionSuccess, EncryptionResult, ApiClientEncryptionService }
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.{ encryptionFailed, missingRequiredField }
import org.corespring.v2.errors.{ V2Error, Field }
import play.api.Logger
import play.api.libs.json.{ Json, JsError, JsSuccess, JsValue }

import scalaz.{ Validation, Failure, Success }
import scalaz.Scalaz._

case class CreateTokenResult(apiClient: String, token: String, settings: JsValue)

class PlayerTokenService(service: ApiClientEncryptionService) {

  private lazy val logger = Logger(classOf[PlayerTokenService])

  def createToken(orgId: ObjectId, json: JsValue): Validation[V2Error, CreateTokenResult] = for {
    accessSettings <- toAccessSettings(json)
    encryptionResult <- service.encryptByOrg(orgId, Json.stringify(Json.toJson(accessSettings)))
      .toSuccess(encryptionFailed(s"orgId: $orgId - Unknown error trying to encrypt"))
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
