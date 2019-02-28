package org.corespring.container.client.pages

import org.corespring.container.client.component.SingleComponentScriptBundle
import org.corespring.container.client.controllers.apps.PageSourceService
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.engine.JadeEngine
import org.corespring.container.client.pages.processing.AssetPathProcessor
import play.api.libs.json.{ JsValue, Json }
import play.api.templates.Html

import scala.concurrent.Future

class RigRenderer(val pageSourceService: PageSourceService,
  containerContext: ContainerExecutionContext,
  jadeEngine: JadeEngine) extends CoreRenderer {

  override def assetPathProcessor: AssetPathProcessor = new AssetPathProcessor {
    override def process(s: String): String = s
  }

  implicit def ec = containerContext.context

  override val name = "rig"

  def render(item: JsValue, bundle: SingleComponentScriptBundle): Future[Html] = Future {

    val (js, css) = prepareJsCss(false, bundle)

    val params: Map[String, Any] = Map(
      "appName" -> "rig",
      "title" -> "rig",
      "js" -> js.toArray,
      "css" -> css.toArray,
      "ngModules" -> (sources.js.ngModules ++ bundle.ngModules).map(s => s"'$s'").mkString(","),
      "ngServiceLogic" -> "",
      "iconSet" -> "emoji",
      "colors" -> Json.stringify(Json.obj()),
      "itemJson" -> Json.prettyPrint(item))
    jadeEngine.renderJade("rig", params)
  }
}
