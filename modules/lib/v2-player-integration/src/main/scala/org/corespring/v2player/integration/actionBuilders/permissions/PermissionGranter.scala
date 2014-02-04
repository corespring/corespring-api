package org.corespring.v2player.integration.actionBuilders.permissions

import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.actionBuilders.permissions.SimpleWildcardChecker.notGrantedMsg

trait PermissionGranter {
  def allow(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Either[String, Boolean]
}

object SimpleWildcardChecker {
  val notGrantedMsg = "Permission not granted"
}

class SimpleWildcardChecker extends PermissionGranter {

  def allow(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Either[String, Boolean] = {

    (options.allowItemId(itemId)
      &&
      sessionId.map(options.allowSessionId(_)).getOrElse(true)
      &&
      options.allowMode(mode)) match {
        case true => Right(true)
        case false => Left(notGrantedMsg)
      }
  }
}
