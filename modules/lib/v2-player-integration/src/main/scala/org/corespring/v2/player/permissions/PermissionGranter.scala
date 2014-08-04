package org.corespring.v2.player.permissions

import org.corespring.v2.auth.models.Mode.Mode
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.log.V2LoggerFactory

trait PermissionGranter {
  def allow(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Either[String, Boolean]
}

object SimpleWildcardChecker {
  val notGrantedMsg = "Permission not granted"
}

class SimpleWildcardChecker extends PermissionGranter {

  lazy val logger = V2LoggerFactory.getLogger("SimpleWildcardChecker")

  import org.corespring.v2.player.permissions.SimpleWildcardChecker._
  def allow(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Either[String, Boolean] = {

    logger.warn("Note: Mode is not being checked at the moment - we need to see if it still applies in v2. see: https://thesib.atlassian.net/browse/CA-1743")

    (options.allowItemId(itemId)
      &&
      sessionId.map(options.allowSessionId(_)).getOrElse(true)) match {
        case true => Right(true)
        case false => {

          logger.trace(s"player options: $options")
          logger.trace(s"itemId? $itemId -> ${options.allowItemId(itemId)}")
          sessionId.foreach { sid => logger.trace(s"sessionId? $sid -> ${options.allowSessionId(sid)}") }
          logger.trace(s"allowMode? $mode -> ${options.allowMode(mode)}")
          Left(notGrantedMsg)
        }
      }
  }
}
