package dev.tools.controllers

import dev.tools.DevTools
import play.api.mvc.Action
import securesocial.core.SecureSocial

object V2Player extends SecureSocial {

  def v2Player(itemId: String) = Action {
    request =>
      if (DevTools.enabled) {
        val call = org.corespring.container.client.controllers.hooks.routes.PlayerHooks.createSessionForItem(itemId)
        SeeOther(s"${call.url}?file=container-player.html").withSession((DevTools.key -> "true"))
      } else NotFound("?")
  }
}
