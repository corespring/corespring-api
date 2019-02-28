package org.corespring.container.client.controllers.apps

import org.corespring.container.client.component.{ ComponentBundler, ComponentJson }
import org.corespring.container.client.controllers.apps.componentEditor.ComponentEditorLaunchingController
import org.corespring.container.client.controllers.{ AssetsController, EditorConfig }
import org.corespring.container.client.hooks.Hooks.{ ItemAndDefaults, StatusMessage }
import org.corespring.container.client.hooks.{ DraftEditorHooks, EditorHooks, ItemEditorHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.{ ComponentEditorRenderer, DevEditorRenderer, EditorRenderer, MainEditorRenderer }
import org.corespring.container.client.views.models.ComponentsAndWidgets
import org.corespring.container.components.model.{ ComponentInfo, Interaction }
import org.corespring.container.components.services.ComponentService
import play.api.Mode.Mode
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.mvc.{ Action, Controller, RequestHeader, SimpleResult }
import play.api.{ Logger, Mode }

import scala.concurrent.Future

trait BaseEditor[H <: EditorHooks]
  extends Controller
  with AssetsController[EditorHooks]
  with ComponentEditorLaunchingController
  with QueryStringHelper
  with PlayerSkinHelper {

  def hooks: H

  def bundler: ComponentBundler

  def renderer: EditorRenderer

  def componentEditorRenderer: ComponentEditorRenderer

  def componentJson: ComponentJson

  def componentService: ComponentService

  def endpoints: Endpoints

  def containerContext: ContainerExecutionContext

  val editorClientOptions: EditorClientOptions

  implicit def ec = containerContext.context

  def config: EditorConfig

  lazy val showNonReleased: Boolean = config.showNonReleased

  lazy val mode: Mode = config.mode

  lazy val componentsAndWidgets = {
    def allow(c: ComponentInfo) = showNonReleased || c.released
    val releasedInteractions = componentService.interactions.filter(allow)
    ComponentsAndWidgets(
      JsArray(releasedInteractions.map(componentJson.toJson)),
      JsArray(componentService.widgets.map(componentJson.toJson)))
  }

  private lazy val logger = Logger(classOf[BaseEditor[H]])

  def load(id: String) = Action.async { implicit request =>
    hooks.load(id).flatMap { e =>

      e.fold(
        err => Future.successful(onError(err)),
        jsonResult => {
          val (json, defaults) = jsonResult
          val prodMode: Boolean = request.getQueryString("mode")
            .map(_ == "prod")
            .getOrElse(mode == Mode.Prod)

          val queryParams = mkQueryParams(m => m)(request)
          val serviceParams = mkQueryParams(mapToJson)
          val encodedComputedColors = calculateColorToken(serviceParams, defaults)
          val computedIconSet = calculateIconSet(serviceParams, defaults)
          val computedColors = calculateColors(serviceParams, defaults)

          val bundle = bundler.bundleAll("editor", Some("editor"), !prodMode, Some(encodedComputedColors)).get
          val mainEndpoints = endpoints.main(id)
          val supportingMaterialsEndpoints = endpoints.supportingMaterials(id)
          renderer.render(
            mainEndpoints,
            supportingMaterialsEndpoints,
            componentsAndWidgets,
            editorClientOptions,
            bundle,
            queryParams,
            prodMode,
            computedIconSet,
            computedColors).map(Ok(_))
        })
    }
  }

  def isProd(rh: RequestHeader) = rh.getQueryString("mode").map(_ == "prod").getOrElse(mode == Mode.Prod)

  private def onError(sm: StatusMessage)(implicit rh: RequestHeader) = {
    import org.corespring.container.client.views.html.error

    val showErrorInUi = isProd(rh)

    val (code, msg) = sm
    code match {
      case SEE_OTHER => SeeOther(msg)
      case _ => Status(code)(error.main(code, msg, showErrorInUi))
    }
  }

  final def findComponentType(json: JsValue): Option[String] = {
    (json \ "components" \\ "componentType").map(_.as[String]).headOption
  }

  def componentEditor(id: String) = Action.async { implicit request =>
    def loadEditor(data: ItemAndDefaults): Future[SimpleResult] = {
      logger.trace(s"function=loadEditor, json=${Json.prettyPrint(data._1)}")
      findComponentType(data._1) match {
        case Some(ct) => componentEditorResult(ct, request, data._2)
        case _ => Future.successful(BadRequest("Can't find a component type"))
      }
    }

    for {
      e <- hooks.load(id)
      result <- e.fold(e => Future.successful(onError(e)), (data) => {
        loadEditor(data)
      })
    } yield result
  }
}

trait BaseItemEditor extends BaseEditor[ItemEditorHooks] {
  override def endpoints: Endpoints = ItemEditorEndpoints
}

trait BaseDraftEditor extends BaseEditor[DraftEditorHooks] {
  override def endpoints: Endpoints = DraftEditorEndpoints
}

class ItemEditor(
  val config: EditorConfig,
  val hooks: ItemEditorHooks,
  val bundler: ComponentBundler,
  val renderer: MainEditorRenderer,
  val componentEditorRenderer: ComponentEditorRenderer,
  val componentJson: ComponentJson,
  val componentService: ComponentService,
  val containerContext: ContainerExecutionContext,
  val editorClientOptions: EditorClientOptions) extends BaseItemEditor

class ItemDevEditor(
  val config: EditorConfig,
  val hooks: ItemEditorHooks,
  val bundler: ComponentBundler,
  val renderer: DevEditorRenderer,
  val componentEditorRenderer: ComponentEditorRenderer,
  val componentJson: ComponentJson,
  val componentService: ComponentService,
  val containerContext: ContainerExecutionContext) extends BaseItemEditor {
  override val editorClientOptions: EditorClientOptions = {
    EditorClientOptions(0, 8 * 1024, 500, StaticPaths.staticPaths)
  }
}

class DraftEditor(
  val config: EditorConfig,
  val hooks: DraftEditorHooks,
  val bundler: ComponentBundler,
  val renderer: MainEditorRenderer,
  val componentEditorRenderer: ComponentEditorRenderer,
  val componentJson: ComponentJson,
  val componentService: ComponentService,
  val containerContext: ContainerExecutionContext,
  val editorClientOptions: EditorClientOptions) extends BaseDraftEditor

class DraftDevEditor(
  val config: EditorConfig,
  val hooks: DraftEditorHooks,
  val bundler: ComponentBundler,
  val renderer: DevEditorRenderer,
  val componentEditorRenderer: ComponentEditorRenderer,
  val componentJson: ComponentJson,
  val componentService: ComponentService,
  val containerContext: ContainerExecutionContext) extends BaseDraftEditor {
  override val editorClientOptions: EditorClientOptions = {
    EditorClientOptions(0, 8 * 1024, 500, StaticPaths.staticPaths)
  }

}
