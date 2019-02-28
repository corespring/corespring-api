package org.corespring.container.client.controllers.launcher.player

import org.corespring.container.client.V2PlayerConfig
import org.corespring.container.client.controllers.launcher._
import org.corespring.container.client.hooks.PlayerLauncherHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import play.api.mvc._

class PlayerLauncher(
  val playerConfig: V2PlayerConfig,
  val containerContext: ContainerExecutionContext,
  val hooks: PlayerLauncherHooks,
  builder: JsBuilder) extends Launcher {

  def componentEditorJs = Action.async { implicit request =>
    hooks.componentEditorJs.map { err =>
      ComponentEditor(playerConfig, request, builder).result(corespringUrl(request))
    }
  }

  def editorJs = Action.async { implicit request =>
    hooks.editorJs.map { implicit js =>
      ItemEditors(playerConfig, request, builder).result(corespringUrl(request))
    }
  }

  def catalogJs = Action.async { implicit request =>
    hooks.catalogJs.map { implicit js =>
      Catalog(playerConfig, request, builder).result(corespringUrl(request))
    }
  }

  def playerJs = Action.async { implicit request =>
    hooks.playerJs.map { implicit js =>
      logger.debug(s"playerJs - isSecure=${js.isSecure}, path=${request.path}, queryString=${request.rawQueryString}")
      Player(playerConfig, request, js, builder).result(corespringUrl(request))
    }
  }

}

