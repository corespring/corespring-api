package org.corespring.container.client.controllers.launcher

import com.softwaremill.macwire.MacwireMacros.wire
import org.corespring.container.client.V2PlayerConfig
import org.corespring.container.client.controllers.launcher.editor.EditorLauncher
import org.corespring.container.client.controllers.launcher.player.PlayerLauncher
import org.corespring.container.client.hooks.PlayerLauncherHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.io.ResourcePath

trait LauncherModules {

  def playerConfig: V2PlayerConfig
  def containerContext: ContainerExecutionContext
  def playerLauncherHooks: PlayerLauncherHooks
  def resourceLoader: ResourcePath

  lazy val jsBuilder: JsBuilder = wire[JsBuilder]

  lazy val editorLauncher: EditorLauncher = wire[EditorLauncher]

  lazy val playerLauncher: PlayerLauncher = wire[PlayerLauncher]

  lazy val launcherControllers = Seq(editorLauncher, playerLauncher)
}
