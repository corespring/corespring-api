package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.{ generalError, missingRequiredField, noJson }
import org.corespring.v2.errors.{ Field, V2Error }
import play.api.libs.json.{ JsValue, Json, JsObject }
import play.api.mvc.Action

import scala.concurrent.Future
import scalaz.{ Success, Failure, Validation }
import scalaz.Scalaz._

case class LaunchInfo(sessionId: String, playerToken: String, apiClient: String, playerJsUrl: String, settings: JsValue)

trait V2SessionService {
  def createExternalModelSession(orgId: ObjectId, model: JsObject): Option[ObjectId]
}

trait ExternalModelLaunchApi extends V2Api {

  def tokenService: PlayerTokenService

  def sessionService: V2SessionService

  def buildExternalLaunchSession = Action.async { implicit request =>
    Future {

      def addDefaults(settings: JsValue, sessionId: ObjectId): Validation[V2Error, JsValue] = {
        (settings \ "sessionId").asOpt[String].map { id =>
          if (id == PlayerAccessSettings.STAR) {
            Success(settings)
          } else {
            Failure(generalError("If you specify 'sessionId' it can only be '*'."))
          }
        }.getOrElse {
          Success(settings.as[JsObject] ++ Json.obj("sessionId" -> sessionId.toString))
        }
      }

      val out: Validation[V2Error, LaunchInfo] = for {
        orgAndOpts <- getOrgIdAndOptions(request)
        externalJson <- request.body.asJson.toSuccess(noJson)
        model <- (externalJson \ "model").asOpt[JsObject].toSuccess(missingRequiredField(Field("model", "object")))
        sessionId <- sessionService.createExternalModelSession(orgAndOpts.orgId, model).toSuccess(generalError("Error creating session"))
        accessSettings <- (externalJson \ "accessSettings").asOpt[JsObject].toSuccess(missingRequiredField(Field("accessSettings", "object")))
        settingsWithDefaults <- addDefaults(accessSettings, sessionId)
        tokenResult <- tokenService.createToken(orgAndOpts.orgId, settingsWithDefaults)
      } yield {
        val url = s"/v2/player/player.js?apiClient=${tokenResult.apiClient}&playerToken=${tokenResult.token}"
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
