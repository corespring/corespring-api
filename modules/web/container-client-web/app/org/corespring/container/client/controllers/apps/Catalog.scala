package org.corespring.container.client.controllers.apps

import org.corespring.container.client.component.ComponentBundler
import org.corespring.container.client.controllers.GetAsset
import org.corespring.container.client.hooks.CatalogHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.CatalogRenderer
import play.api.Mode.Mode
import play.api.mvc.{ Action, AnyContent, Controller }
import play.api.{ Logger, Mode }

import scala.concurrent.Future

class Catalog(
  mode: Mode,
  val hooks: CatalogHooks,
  catalogRenderer: CatalogRenderer,
  bundler: ComponentBundler,
  val containerContext: ContainerExecutionContext)
  extends Controller
  with GetAsset[CatalogHooks]
  with QueryStringHelper
  with PlayerSkinHelper {

  private lazy val logger = Logger(classOf[Catalog])

  private lazy val endpoints = ItemEditorEndpoints

  def load(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      val prodMode = request.getQueryString("mode").map(_ == "prod").getOrElse(mode == Mode.Prod)
      val queryParams = mkQueryParams(mapToJson)

      hooks.showCatalog(id).flatMap { err =>
        err match {
          case Left((code, msg)) => {
            val showErrorInUi = !prodMode
            Future.successful(
              Status((code))(org.corespring.container.client.views.html.error.main(code, msg, showErrorInUi)))
          }
          case Right(defaults) => {
            val encodedComputedColors = calculateColorToken(queryParams, defaults)
            val computedIconSet = calculateIconSet(queryParams, defaults)
            val computedColors = calculateColors(queryParams, defaults)

            bundler.bundleAll("catalog", Some("editor"), !prodMode, Some(encodedComputedColors)) match {
              case Some(b) => {
                val mainEndpoints = endpoints.main(id)
                val supportingMaterialsEndpoints = endpoints.supportingMaterials(id)
                val queryParams = mkQueryParams(m => m)
                catalogRenderer.render(b, mainEndpoints, supportingMaterialsEndpoints, queryParams, prodMode, computedIconSet, computedColors).map { html =>
                  Ok(html)
                }
              }
              case _ => Future.successful(BadRequest("Failed to build bundle"))
            }
          }
        }
      }
  }
}
