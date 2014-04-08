package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.v2player.integration.actionBuilders.access.{ V2PlayerCookieReader, PlayerOptions }
import play.api.mvc.{ RequestHeader, AnyContent, Request }
import org.slf4j.LoggerFactory

trait LoadOrgAndOptions extends UserSession with V2PlayerCookieReader {

  protected lazy val logger = LoggerFactory.getLogger("v2Player.org-id-options")

  def getOrgIdAndOptions(request: RequestHeader): Option[(ObjectId, PlayerOptions)] = {
    logger.trace(s"[getOrgIdAndOptions]: ${request.cookies}")
    userFromSession(request).map(
      u => {
        logger.trace(s"found user: $u")
        (u.org.orgId, PlayerOptions.ANYTHING)
      }) orElse anonymousUser(request)
  }

  def anonymousUser(request: RequestHeader): Option[(ObjectId, PlayerOptions)] = {
    logger.trace(s"parse anonymous user")
    for {
      orgId <- orgIdFromCookie(request)
      options <- renderOptions(request)
    } yield {
      (new ObjectId(orgId), options)
    }
  }
}

