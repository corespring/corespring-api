package org.corespring.v2player.integration.actionBuilders.permissions

import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions

trait PermissionGranter {
  def allow(itemId: String, sessionId: Option[String], options: PlayerOptions ) : Either[String,Boolean]
}

class SimpleWildcardChecker extends PermissionGranter{
  def allow(itemId: String, sessionId: Option[String], options: PlayerOptions): Either[String, Boolean] = {

    (options.allowItemId(itemId) && sessionId.map(options.allowSessionId(_)).getOrElse(true)) match {
      case true => Right(true)
      case false => Left("Permission not granted")
    }
  }
}
