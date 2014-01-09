package org.corespring.poc.integration.impl.actionBuilders.access

import play.api.mvc.{AnyContent, Request}
import org.corespring.player.accessControl.cookies.PlayerCookieReader

class PermissionsInCookieDecider extends AccessDecider with PlayerCookieReader{

  def accessForItemId(itemId: String, request: Request[AnyContent]): AccessResult = {


    orgIdFromCookie(request).map{ orgId =>


    }

  }

  def accessForSessionId(sessionId: String, request: Request[AnyContent]): AccessResult = ???
}
