package org.corespring.v2.api

import org.corespring.models.Organization
import org.corespring.v2.actions.V2Actions
import org.corespring.v2.api.services.{ CreateTokenResult, PlayerTokenService }
import org.corespring.v2.errors.Errors.{ encryptionFailed, noJson }
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.Controller

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Scalaz._
import scalaz.Validation

class PlayerTokenApi(
  actions: V2Actions,
  tokenService: PlayerTokenService,
  v2ApiContext: V2ApiExecutionContext)
  extends Controller
  with ValidationToResultLike {

  implicit def ec: ExecutionContext = v2ApiContext.context

  private lazy val logger = Logger(classOf[PlayerTokenApi])

  def encryptionFailedError(org: Organization) = encryptionFailed(s"orgId: ${org.id} orgName: ${org.name} - Unknown error trying to encrypt")

  /**
   * Creates a player token.
   * param json - access settings in the json body
   * If the json doesn't specify any of the AccessSetting properties, an error will be returned.
   * If they specify at a minimum the required 'expires' property,
   * The remaining properties will be set to a wildcard value.
   * return json - playerToken, clientId and accessSettings used
   *
   * @see PlayerAccessSettings
   */
  def createPlayerToken = actions.OrgAndApiClient.async { request =>

    logger.debug(s"function=createPlayerToken")

    Future {
      val out: Validation[V2Error, CreateTokenResult] = for {
        json <- request.body.asJson.toSuccess(noJson)
        result <- tokenService.createToken(request.apiClient, json)
      } yield result

      out.map {
        case CreateTokenResult(apiClient, token, json) => {
          logger.debug(s"function=createPlayerToken apiClient=$apiClient accessSettings=${Json.stringify(json)} token=$token")
          Json.obj(
            "playerToken" -> token,
            "apiClient" -> apiClient,
            "accessSettings" -> json)
        }
      }.toSimpleResult()
    }
  }

}
