package org.corespring.container.client.controllers.launcher.editor

import org.corespring.container.client.V2PlayerConfig
import org.corespring.container.client.controllers.launcher.{ JsBuilder, ItemEditors, Launcher }
import org.corespring.container.client.hooks.PlayerLauncherHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import play.api.mvc.Action

class EditorLauncher(val playerConfig: V2PlayerConfig,
  val containerContext: ContainerExecutionContext,
  val hooks: PlayerLauncherHooks,
  builder: JsBuilder) extends Launcher {

  def editorJs = Action.async { implicit request =>
    hooks.editorJs.map { implicit js =>
      ItemEditors(playerConfig, request, builder).result(corespringUrl(request))
    }
  }
}
