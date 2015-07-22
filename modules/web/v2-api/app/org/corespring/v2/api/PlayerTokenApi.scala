package org.corespring.v2.api

import org.corespring.models.Organization
import org.corespring.v2.api.services.{ CreateTokenResult, PlayerTokenService }
import org.corespring.v2.errors.Errors.{ encryptionFailed, noJson }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json._
import play.api.mvc.Action

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.Validation

trait PlayerTokenApi extends V2Api {

  private lazy val logger = V2LoggerFactory.getLogger("PlayerTokenApi")

  def tokenService: PlayerTokenService

  def encryptionFailedError(org: Organization) = encryptionFailed(s"orgId: ${org.id} orgName: ${org.name} - Unknown error trying to encrypt")

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

    Future {
      val out: Validation[V2Error, CreateTokenResult] = for {
        identity <- getOrgAndOptions(request)
        json <- request.body.asJson.toSuccess(noJson)
        result <- tokenService.createToken(identity.org.id, json)
      } yield result

      validationToResult[CreateTokenResult] {
        case CreateTokenResult(apiClient, token, json) => {
          logger.debug(s"function=createPlayerToken apiClient=$apiClient accessSettings=${Json.stringify(json)} token=$token")
          Ok(
            Json.obj(
              "playerToken" -> token,
              "apiClient" -> apiClient,
              "accessSettings" -> json))
        }
      }(out)
    }
  }
}
