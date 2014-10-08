package org.corespring.v2.api

import org.corespring.platform.core.encryption.{ EncryptionFailure, EncryptionResult, EncryptionSuccess, OrgEncrypter }
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.{ encryptionFailed, generalError, noJson }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.Json
import play.api.mvc.Action

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait PlayerTokenApi extends V2Api {

  private lazy val logger = V2LoggerFactory.getLogger("PlayerTokenApi")

  def encrypter: OrgEncrypter

  def createPlayerToken = Action.async { request =>

    logger.debug(s"function=createPlayerToken")

    def encryptionToValidation(er: EncryptionResult): Validation[V2Error, String] = {
      er match {
        case EncryptionSuccess(clientId, encrypted, requested) => {
          logger.trace(s"function=createPlayerToken clientId=$clientId request=$requested")
          Success(encrypted)
        }
        case EncryptionFailure(msg, e) => {
          logger.trace(s"function=createPlayerToken - encryption failed")
          Failure(encryptionFailed(msg))
        }
      }
    }

    Future {
      val out: Validation[V2Error, String] = for {
        json <- request.body.asJson.toSuccess(noJson)
        accessSettings <- json.asOpt[PlayerAccessSettings].toSuccess(generalError("Can't parse json as PlayerAccessSettings"))
        identity <- getOrgIdAndOptions(request)
        encryptionResult <- encrypter.encrypt(identity.orgId, Json.stringify(json)).toSuccess(encryptionFailed(s"orgId: ${identity.orgId} - Unknown error trying to encrypt"))
        code <- encryptionToValidation(encryptionResult)
      } yield {
        code
      }
      validationToResult[String] { code => Ok(Json.obj("playerToken" -> code)) }(out)
    }
  }
}
