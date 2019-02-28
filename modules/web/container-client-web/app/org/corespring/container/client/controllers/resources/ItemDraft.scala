package org.corespring.container.client.controllers.resources

import org.corespring.container.client.controllers.helpers.{ ItemCleaner, ItemInspector, PlayerXhtml }
import org.corespring.container.client.hooks._
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.components.services.ComponentService
import play.api.Logger
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.libs.json.Json._
import play.api.mvc._

import scala.concurrent.Future

object ItemDraft {
  object Errors {
    val noJson = "No json in request body"
    val unknownSubset = "unknown subset"
    val errorSaving = "Error Saving"
    val invalidXhtml = "Invalid xhtml"
  }
}

class ItemDraft(
  val containerContext: ContainerExecutionContext,
  componentService: ComponentService,
  val hooks: CoreItemHooks with DraftHooks,
  val playerXhtml: PlayerXhtml,
  val itemInspector: ItemInspector,
  val materialHooks: ItemDraftSupportingMaterialHooks) extends CoreItem {

  override lazy val logger = Logger(classOf[ItemDraft])

  override protected def componentTypes: Seq[String] = componentService.components.map(_.componentType)

  def createItemAndDraft = Action.async {
    implicit request =>
      val collectionId = request.body.asJson.flatMap(json => (json \ "collectionId").asOpt[String])
      hooks.createItemAndDraft(collectionId).map { either =>
        either match {
          case Left(sm) => sm
          case Right((itemId, draftName)) => Ok(obj("itemId" -> itemId, "draftName" -> draftName))
        }
      }
  }

  def createWithSingleComponent(componentType: String) = Action.async { implicit request =>

    val defaultData = componentService.interactions
      .find(_.componentType == componentType)
      .map { _.defaultData }
      .flatMap { case o: JsObject => Some(o); case _ => None }

    val collectionId = request.body.asJson.flatMap(json => (json \ "collectionId").asOpt[String])

    defaultData.map { d =>
      hooks.createSingleComponentItemDraft(collectionId, componentType, SingleComponent.Key, d).map { either =>
        either match {
          case Left(sm) => toResult(sm)
          case Right((itemId, draftName)) => Created(obj("itemId" -> itemId, "draftName" -> draftName))
        }
      }
    }.getOrElse(Future.successful(NotFound(obj("error" -> s"unknown componentType: $componentType"))))
  }

  def save(draftId: String) = Action.async { implicit request: Request[AnyContent] =>
    request.body.asJson match {
      case Some(json) =>
        val xhtml = (json \ "xhtml").asOpt[String]
        val components = (json \ "components").asOpt[JsObject]

        val cleanedJson = if (!xhtml.isEmpty && !components.isEmpty) {
          val cleanedComponents = ItemCleaner.cleanComponents(xhtml.get, components.get)
          json.as[JsObject] ++ Json.obj("components" -> cleanedComponents)
        } else {
          json
        }
        hooks.save(draftId, cleanedJson).map {
          _ match {
            case Left(sm) => sm
            case Right(json) => Ok(cleanedJson)
          }
        }
      case _ => Future { BadRequest(obj("error" -> ItemDraft.Errors.noJson)) }
    }
  }

  def commit(draftId: String) = Action.async { implicit request =>
    val force = request.getQueryString("force").exists(_ == "true")
    hooks.commit(draftId, force).map { e =>
      e match {
        case Left(sm) => sm
        case Right(json) => Ok(json)
      }
    }
  }

}
