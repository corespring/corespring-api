package org.corespring.container.client.controllers.apps

import org.corespring.container.client.component.ComponentBundler
import org.corespring.container.client.controllers.apps.componentEditor.ComponentEditorLaunchingController
import org.corespring.container.client.hooks.{ CatalogHooks, ComponentEditorHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.ComponentEditorRenderer
import play.api.Mode.Mode
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

class ComponentEditor(
  val containerContext: ContainerExecutionContext,
  val componentEditorRenderer: ComponentEditorRenderer,
  val bundler: ComponentBundler,
  val mode: Mode,
  val hooks: ComponentEditorHooks) extends Controller with ComponentEditorLaunchingController {

  def load(componentType: String) = Action.async { request =>
    hooks.load(componentType)(request).flatMap { err =>
      err match {
        case Right((item, defaults)) =>
          componentEditorResult(componentType, request, defaults)
      }
    }
  }

}
