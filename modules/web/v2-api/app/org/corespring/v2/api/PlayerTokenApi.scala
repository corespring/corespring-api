package org.corespring.v2.api

import org.corespring.platform.core.encryption.{ EncryptionFailure, EncryptionResult, EncryptionSuccess, OrgEncrypter }
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.{ missingRequiredField, encryptionFailed, generalError, noJson }
import org.corespring.v2.errors.{ Field, V2Error }
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json._
import play.api.mvc.Action

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait PlayerTokenApi extends V2Api {

  private lazy val logger = V2LoggerFactory.getLogger("PlayerTokenApi")

  def encrypter: OrgEncrypter

  /**
   * Creates a player token.
   * param json - access settings in the json body
   * If the json doesn't specify any of the AccessSetting properties, an error will be returned.
   * If they specify at a minimum the required 'expires' property,
   * The remaining properties will be set to a wildcard value.
   * return json - playerToken, clientId and accessSettings used
   * @see PlayerAccessSettings
   */
  def createPlayerToken = Action.async { request =>

    logger.debug(s"function=createPlayerToken")

    def encryptionToValidation(er: EncryptionResult): Validation[V2Error, (String, String)] = {
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

    def toAccessSettings(json: JsValue): Validation[V2Error, PlayerAccessSettings] = PlayerAccessSettings.permissiveRead(json) match {
      case JsError(_) => Failure(missingRequiredField(Field("expires", "number")))
      case JsSuccess(o, _) => Success(o)
    }

    Future {
      val out: Validation[V2Error, (String, String, JsValue)] = for {
        identity <- getOrgIdAndOptions(request)
        json <- request.body.asJson.toSuccess(noJson)
        accessSettings <- toAccessSettings(json)
        encryptionResult <- encrypter.encrypt(identity.orgId, Json.stringify(Json.toJson(accessSettings))).toSuccess(encryptionFailed(s"orgId: ${identity.orgId} - Unknown error trying to encrypt"))
        clientIdAndToken <- encryptionToValidation(encryptionResult)
      } yield {
        (clientIdAndToken._1, clientIdAndToken._2, Json.toJson(accessSettings))
      }

      validationToResult[(String, String, JsValue)] { triplet =>
        val (clientId, token, json) = triplet
        logger.debug(s"function=createPlayerToken apiClient=$clientId accessSettings=${Json.stringify(json)} token=$token")
        Ok(
          Json.obj(
            "playerToken" -> token,
            "apiClient" -> clientId,
            "accessSettings" -> json))
      }(out)
    }
  }
}
