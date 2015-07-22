package org.corespring.v2.auth

import grizzled.slf4j.Logger
import org.corespring.common.config.AppConfig
import org.corespring.v2.auth.models.Mode.Mode
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.permissionNotGranted
import org.corespring.v2.errors.V2Error
import play.api.libs.json.Json

import scalaz.{ Success, Failure, Validation }

class AccessSettingsWildcardCheck(appConfig: AppConfig) {

  private lazy val logger = Logger(classOf[AccessSettingsWildcardCheck])

  def notGrantedError(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings) = {
    permissionNotGranted(
      Seq(
        "Permission not granted",
        s"itemId ($itemId) allowed? ${settings.allowItemId(itemId)}",
        s"sessionId: ($sessionId) allowed? ${sessionId.map { settings.allowSessionId(_) }.getOrElse(true)}",
        "mode: allowed? true",
        s"Options: ${Json.toJson(settings)}")
        .mkString(", "))
  }

  def allow(itemId: String, sessionId: Option[String], mode: Mode, settings: PlayerAccessSettings): Validation[V2Error, Boolean] = appConfig.allowAllSessions match {
    case true => Success(true)
    case _ => {
      logger.warn("Note: Mode is not being checked at the moment - we need to see if it still applies in v2. see: https://thesib.atlassian.net/browse/CA-1743")

      val result = (settings.allowItemId(itemId)
        &&
        sessionId.map(settings.allowSessionId(_)).getOrElse(true)) match {
          case true => Success(true)
          case false => {
            logger.trace(s"player options: $settings")
            logger.trace(s"itemId? $itemId -> ${settings.allowItemId(itemId)}")
            sessionId.foreach { sid => logger.trace(s"sessionId? $sid -> ${settings.allowSessionId(sid)}") }
            logger.trace(s"allowMode? $mode -> ${settings.allowMode(mode)}")
            Failure(notGrantedError(itemId, sessionId, settings))
          }
        }
      result
    }
  }
}

object AccessSettingsWildcardCheck extends AccessSettingsWildcardCheck(AppConfig)