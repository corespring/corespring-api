package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.item.Item
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.v2.auth.models.{ IdentityJson, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors.{ generalError, missingRequiredField, noJson }
import org.corespring.v2.errors.{ Field, V2Error }
import org.corespring.v2.sessiondb.SessionServices
import play.api.libs.json.{ JsValue, Json, JsObject }
import play.api.mvc.{ RequestHeader, Action }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Success, Failure, Validation }
import scalaz.Scalaz._

case class LaunchInfo(sessionId: String, playerToken: String, apiClient: String, playerJsUrl: String, settings: JsValue)

case class ExternalModelLaunchConfig(playerJsUrl: String)
class ExternalModelLaunchApi(
  tokenService: PlayerTokenService,
  sessionServices: SessionServices,
  config: ExternalModelLaunchConfig,
  v2ApiContext: V2ApiExecutionContext,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {

  override implicit def ec: ExecutionContext = v2ApiContext.context

  def badSessionIdError = generalError("If you specify 'sessionId' it can only be '*'.")
  def createSessionError = generalError("Error creating session")

  def buildExternalLaunchSession = Action.async { implicit request =>
    Future {

      def addDefaults(settings: JsValue, sessionId: ObjectId): Validation[V2Error, JsValue] = {
        (settings \ "sessionId").asOpt[String].map { id =>
          if (id == PlayerAccessSettings.STAR) {
            Success(settings)
          } else {
            Failure(badSessionIdError)
          }
        }.getOrElse {
          Success(settings.as[JsObject] ++ Json.obj("sessionId" -> sessionId.toString))
        }
      }

      def inlineItem(orgAndOpts: OrgAndOpts, item: JsValue) = Json.obj(
        "identity" -> IdentityJson(orgAndOpts),
        "item" -> item)

      val out: Validation[V2Error, LaunchInfo] = for {
        orgAndOpts <- getOrgAndOptions(request)
        externalJson <- request.body.asJson.toSuccess(noJson)
        model <- (externalJson \ "model").asOpt[JsObject].toSuccess(missingRequiredField(Field("model", "object")))
        sessionId <- sessionServices.main.create(inlineItem(orgAndOpts, model)).toSuccess(generalError("Error creating session"))
        accessSettings <- (externalJson \ "accessSettings").asOpt[JsObject].toSuccess(missingRequiredField(Field("accessSettings", "object")))
        settingsWithDefaults <- addDefaults(accessSettings, sessionId)
        tokenResult <- tokenService.createToken(orgAndOpts.org.id, settingsWithDefaults)
      } yield {
        val url = s"${config.playerJsUrl}?apiClient=${tokenResult.apiClient}&playerToken=${tokenResult.token}"
        LaunchInfo(sessionId.toString, tokenResult.token, tokenResult.apiClient, url, tokenResult.settings)
      }

      validationToResult[LaunchInfo](i =>
        Ok(
          Json.obj(
            "sessionId" -> i.sessionId,
            "playerToken" -> i.playerToken,
            "playerJsUrl" -> i.playerJsUrl,
            "settings" -> i.settings)))(out)
    }
  }

}
