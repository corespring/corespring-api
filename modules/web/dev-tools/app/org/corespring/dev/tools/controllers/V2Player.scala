package org.corespring.dev.tools.controllers

import org.corespring.dev.tools.DevTools
import play.api.mvc.{ Controller, Action }

object V2Player extends Controller {

  def v2Player(itemId: String) = Action {
    request =>
      if (DevTools.enabled) {
        val call = org.corespring.container.client.controllers.apps.routes.Player.createSessionForItem(itemId)
        SeeOther(s"${call.url}?file=container-player.html").withSession((DevTools.key -> "true"))
      } else NotFound("?")
  }
}
