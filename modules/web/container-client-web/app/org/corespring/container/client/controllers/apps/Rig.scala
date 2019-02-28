package org.corespring.container.client.controllers.apps

import org.corespring.container.client.component.ComponentBundler
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.RigRenderer
import org.corespring.container.components.model.ComponentInfo
import org.corespring.container.components.services.ComponentService
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, Controller }

import scala.concurrent.Future

class Rig(
  rigRenderer: RigRenderer,
  bundler: ComponentBundler,
  componentService: ComponentService,
  containerContext: ContainerExecutionContext) extends Controller {

  implicit def ec = containerContext.context

  private def loadData(componentType: String, dataName: String): JsValue = {
    val data = for {
      c <- component(componentType)
      filename <- Some(if (dataName.endsWith(".json")) dataName else s"$dataName.json")
      d <- c.sampleData.get(filename)
    } yield d
    data.map((_ \ "item")).getOrElse(Json.obj())
  }

  private def component(componentType: String): Option[ComponentInfo] = {
    (componentService.interactions ++ componentService.widgets).find(c => c.componentType == componentType)
  }

  def asset(componentType: String, file: String) = controllers.Assets.at("/container-client", file)

  def load(componentType: String): Action[AnyContent] = Action.async { implicit request =>
    request.getQueryString("data") match {
      case Some(d) => {
        val item = loadData(componentType, d)
        val bundle = bundler.singleBundle(componentType, "rig", true).get
        rigRenderer.render(item, bundle).map { html =>
          Ok(html)
        }
      }
      case _ => Future.successful(BadRequest("You need to specify a json file using 'data' query param"))
    }
  }
}
