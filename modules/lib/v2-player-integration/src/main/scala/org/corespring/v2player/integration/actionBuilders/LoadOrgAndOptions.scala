package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.v2player.integration.actionBuilders.access.{ V2PlayerCookieReader, PlayerOptions }
import play.api.mvc.{ AnyContent, Request }

trait LoadOrgAndOptions extends UserSession with V2PlayerCookieReader {

  def getOrgIdAndOptions(request: Request[AnyContent]): Option[(ObjectId, PlayerOptions)] = userFromSession(request).map(
    u => {
      (u.org.orgId, PlayerOptions.ANYTHING)
    }) orElse anonymousUser(request)

  def anonymousUser(request: Request[AnyContent]): Option[(ObjectId, PlayerOptions)] = {
    for {
      orgId <- orgIdFromCookie(request)
      options <- renderOptions(request)
    } yield {
      (new ObjectId(orgId), options)
    }
  }
}

