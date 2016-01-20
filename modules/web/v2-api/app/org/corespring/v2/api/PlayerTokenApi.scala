package org.corespring.v2.api

import org.corespring.models.Organization
import org.corespring.models.auth.ApiClient
import org.corespring.v2.api.services.{ CreateTokenResult, PlayerTokenService }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{ encryptionFailed, noJson }
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{ Controller, RequestHeader, Action }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Scalaz._
import scalaz.Validation

class PlayerTokenApi(
  tokenService: PlayerTokenService,
  v2ApiContext: V2ApiExecutionContext,
  val identifyFn: RequestHeader => Validation[V2Error, (OrgAndOpts, ApiClient)])
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
   * @see PlayerAccessSettings
   */
  def createPlayerToken = Action.async { request =>

    logger.debug(s"function=createPlayerToken")

    Future {
      val out: Validation[V2Error, CreateTokenResult] = for {
        client <- identifyFn(request).map(_._2)
        json <- request.body.asJson.toSuccess(noJson)
        result <- tokenService.createToken(client, json)
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
