package org.corespring.v2.player.permissions

import org.corespring.v2.auth.models.Mode.Mode
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.Json

trait PermissionGranter {
  def allow(itemId: String, sessionId: Option[String], mode: Mode, settings: PlayerAccessSettings): Either[String, Boolean]
}

object SimpleWildcardChecker {
  def notGrantedMsg(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings) = {
    s"Permission not granted: itemId ($itemId) allowed? ${settings.allowItemId(itemId)}, sessionId: ($sessionId) allowed? ${sessionId.map { settings.allowSessionId(_) }.getOrElse(true)}, mode: allowed? true. Options: ${Json.toJson(settings)}"
  }
}

class SimpleWildcardChecker extends PermissionGranter {

  lazy val logger = V2LoggerFactory.getLogger("SimpleWildcardChecker")

  import org.corespring.v2.player.permissions.SimpleWildcardChecker._
  def allow(itemId: String, sessionId: Option[String], mode: Mode, settings: PlayerAccessSettings): Either[String, Boolean] = {

    logger.warn("Note: Mode is not being checked at the moment - we need to see if it still applies in v2. see: https://thesib.atlassian.net/browse/CA-1743")

    (settings.allowItemId(itemId)
      &&
      sessionId.map(settings.allowSessionId(_)).getOrElse(true)) match {
        case true => Right(true)
        case false => {
          logger.trace(s"player options: $settings")
          logger.trace(s"itemId? $itemId -> ${settings.allowItemId(itemId)}")
          sessionId.foreach { sid => logger.trace(s"sessionId? $sid -> ${settings.allowSessionId(sid)}") }
          logger.trace(s"allowMode? $mode -> ${settings.allowMode(mode)}")
          Left(notGrantedMsg(itemId, sessionId, settings))
        }
      }
  }
}
