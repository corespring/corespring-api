package org.corespring.container.client.integration

import java.net.URL

import com.softwaremill.macwire.MacwireMacros.wire
import org.corespring.container.client.V2PlayerConfig
import org.corespring.container.client.component._
import org.corespring.container.client.controllers._
import org.corespring.container.client.controllers.apps._
import org.corespring.container.client.controllers.helpers.LoadClientSideDependencies
import org.corespring.container.client.controllers.launcher.{ LauncherModules }
import org.corespring.container.client.controllers.resources._
import org.corespring.container.client.integration.validation.Validator
import org.corespring.container.client.io.ResourcePath
import org.corespring.container.client.pages.engine.{ JadeEngine, JadeEngineConfig }
import org.corespring.container.client.pages.processing.AssetPathProcessor
import org.corespring.container.components.services.{ ComponentService, DependencyResolver }
import org.corespring.container.js.{ JsProcessingConfig, JsProcessingModule }
import play.api.Mode
import play.api.Mode.Mode
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

case class ContainerExecutionContext(context: ExecutionContext)

object ContainerExecutionContext {
  val TEST = ContainerExecutionContext(ExecutionContext.global)
}

object DefaultIntegration {
  val pathsThatNeedResolution = Seq(
    "components/",
    "component-sets/",
    "editor",
    "-prod",
    "player.min")
}

case class ContainerConfig(
  mode: Mode,
  showNonReleasedComponents: Boolean,
  editorDebounceInMillis: Long,
  components: ComponentsConfig,
  player: V2PlayerConfig,
  uploadAudioMaxSizeKb: Long,
  uploadImageMaxSizeKb: Long)

trait DefaultIntegration
  extends ControllersModule
  with ComponentControllersModule
  with ResourcesModule
  with LauncherModules
  with JsProcessingModule {

  lazy val defaultIntegrationControllers: Seq[Controller] = {
    containerMainControllers ++
      resourceControllers ++
      launcherControllers ++
      componentControllers
  }

  def containerConfig: ContainerConfig

  override final lazy val editorConfig = EditorConfig(containerConfig.mode, containerConfig.showNonReleasedComponents)

  override lazy val editorClientOptions = {
    EditorClientOptions(containerConfig.editorDebounceInMillis, containerConfig.uploadAudioMaxSizeKb, containerConfig.uploadImageMaxSizeKb, StaticPaths.staticPaths)
  }

  def jadeEngineConfig: JadeEngineConfig = JadeEngineConfig("container-client/jade", containerConfig.mode, resourceLoader.loadPath(_), resourceLoader.lastModified(_))

  def containerContext: ContainerExecutionContext

  override lazy val componentService: ComponentService = new DefaultComponentService(containerConfig.mode, components)

  val loadResource: String => Option[URL]

  override lazy val resourceLoader = new ResourcePath(loadResource)

  /**
   * For a given resource path return a resolved path.
   * By default this just returns the path, so no domain is used.
   * Override it if you want to make use of it.
   */
  def resolveDomain(path: String): String = path

  def validate: Either[String, Boolean] = {
    Validator.absolutePathInProdMode(containerConfig.components.componentsPath)
  }

  override lazy val jsProcessingConfig: JsProcessingConfig = JsProcessingConfig(containerConfig.mode == Mode.Dev)
  override lazy val playerConfig: V2PlayerConfig = containerConfig.player
  override lazy val componentsConfig: ComponentsConfig = containerConfig.components
  override lazy val assetPathProcessor = new AssetPathProcessor {

    override def process(s: String): String = {
      if (DefaultIntegration.pathsThatNeedResolution.exists(s.contains(_))) {
        resolveDomain(s)
      } else s
    }
  }

  lazy val pageSourceServiceConfig: PageSourceServiceConfig = PageSourceServiceConfig(
    v2Player.Routes.prefix,
    containerConfig.mode == Mode.Dev,
    resourceLoader.loadPath(_))

  override lazy val pageSourceService: PageSourceService = wire[JsonPageSourceService]

  override lazy val componentJson: ComponentJson = new ComponentInfoJson(v2Player.Routes.prefix)

  override lazy val dependencyResolver: DependencyResolver = wire[DependencyResolver]

  lazy val clientSideDependencies: LoadClientSideDependencies = new LoadClientSideDependencies {}

  override lazy val componentBundler: ComponentBundler = wire[DefaultComponentBundler]

  override lazy val jadeEngine = wire[JadeEngine]

  override def sessionExecutionContext = SessionExecutionContext(
    DefaultIntegration.this.containerContext.context,
    DefaultIntegration.this.containerContext.context)

}
