package org.corespring.container.client.pages

import org.corespring.container.client.V2PlayerConfig
import org.corespring.container.client.component.{ ComponentJson, ComponentsScriptBundle }
import org.corespring.container.client.controllers.apps.{ PageSourceService, StaticPaths }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.engine.JadeEngine
import org.corespring.container.client.pages.processing.AssetPathProcessor
import org.corespring.container.client.views.models.{ MainEndpoints, SupportingMaterialsEndpoints }
import org.corespring.container.client.views.txt.js.CatalogServices
import org.corespring.container.components.services.ComponentService
import play.api.libs.json.{ JsObject, Json }
import play.api.templates.Html

import scala.concurrent.Future

class CatalogRenderer(
  playerConfig: V2PlayerConfig,
  jadeEngine: JadeEngine,
  containerContext: ContainerExecutionContext,
  val pageSourceService: PageSourceService,
  componentJson: ComponentJson,
  componentService: ComponentService,
  val assetPathProcessor: AssetPathProcessor) extends CoreRenderer {

  implicit def ec = containerContext.context

  override val name = "catalog"

  private def javaBoolean(b: Boolean): java.lang.Boolean = new java.lang.Boolean(b)

  def render(bundle: ComponentsScriptBundle,
    mainEndpoints: MainEndpoints,
    supportingMaterialsEndpoints: SupportingMaterialsEndpoints,
    queryParams: Map[String, String],
    prodMode: Boolean,
    iconSet: String,
    colors: JsObject): Future[Html] = Future {

    val (js, css) = prepareJsCss(prodMode, bundle)

    val componentSet = Json.toJson(componentService.interactions.map(componentJson.toJson))
    val queryParamsJson = Json.toJson(queryParams)
    val ngServiceLogic = CatalogServices(s"$name-injected", componentSet, mainEndpoints, supportingMaterialsEndpoints, queryParamsJson).toString
    val newRelicRumConfig = playerConfig.newRelicRumConfig.map(c => c.json).getOrElse(Json.obj())
    val newRelicRumScriptPath = playerConfig.newRelicRumConfig.map(c => c.scriptPath).getOrElse("")

    val params: Map[String, Any] = Map(
      "appName" -> name,
      "js" -> js.toArray,
      "css" -> css.toArray,
      "iconSet" -> iconSet,
      "colors" -> colors,
      "ngModules" -> jsArrayString(Some(s"$name-injected") ++ sources.js.ngModules ++ bundle.ngModules),
      "ngServiceLogic" -> ngServiceLogic,
      "staticPaths" -> Json.stringify(StaticPaths.staticPaths),
      "newRelicRumEnabled" -> javaBoolean(playerConfig.useNewRelic),
      "newRelicRumScriptPath" -> newRelicRumScriptPath,
      "newRelicRumConfig" -> Json.stringify(newRelicRumConfig))
    jadeEngine.renderJade(name, params)
  }
}
