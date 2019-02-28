package org.corespring.container.client.controllers.launcher

import org.corespring.container.client.hooks.PlayerLauncherHooks
import org.corespring.container.client.{ HasContainerContext, V2PlayerConfig }
import org.corespring.container.logging.ContainerLogger
import play.api.mvc._

object BaseUrl {
  def apply(r: RequestHeader): String = {

    /**
     * Note: You can't check a request to see if its http or not in Play
     * But even if you could you may be sitting behind a reverse proxy.
     * see: https://groups.google.com/forum/?fromgroups=#!searchin/play-framework/https$20request/play-framework/11zbMtNI3A8/o4318Z-Ir6UJ
     * but the tip was to check for the header below
     */
    val protocol = r.headers.get("x-forwarded-proto") match {
      case Some("https") => "https"
      case _ => "http"
    }

    protocol + "://" + r.host
  }
}

trait Launcher extends Controller with HasContainerContext {

  def hooks: PlayerLauncherHooks
  def playerConfig: V2PlayerConfig

  def corespringUrl(request: RequestHeader) = {
    playerConfig.rootUrl.getOrElse(BaseUrl(request))
  }

  lazy val logger = ContainerLogger.getLogger("PlayerLauncher")
}

