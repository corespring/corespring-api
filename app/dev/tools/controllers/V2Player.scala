package dev.tools.controllers

import org.corespring.container.client.controllers.hooks.PlayerHooks
import play.api.mvc.{ AnyContentAsEmpty, Request, AnyContent, Action }
import play.api.{ Mode, Play }
import scala.concurrent.Await
import securesocial.core.SecureSocial

object V2Player extends SecureSocial {

  def v2Player(itemId: String) = SecuredAction {
    request =>

      def devToolsEnabled = Play.current.mode == Mode.Dev || Play.current.configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)

      if (devToolsEnabled) {
        import play.api.Play.current
        import play.api.Play.global
        import scala.concurrent.duration._
        val playerHooks = global.getControllerInstance(classOf[PlayerHooks])
        val action: Action[AnyContent] = playerHooks.createSessionForItem(itemId)
        val duration = 3.second

        val withPath = Request(request.copy(queryString = Map("file" -> Seq("container-player.html"))), AnyContentAsEmpty)
        Await.result(action(withPath), duration)
      } else NotFound("?")
  }
}
