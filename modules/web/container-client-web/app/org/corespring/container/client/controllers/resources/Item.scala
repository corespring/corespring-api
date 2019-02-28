package org.corespring.container.client.controllers.resources

import org.corespring.container.client.controllers.helpers.{ ItemInspector, PlayerXhtml }
import org.corespring.container.client.hooks.{ CoreItemHooks, CreateItemHook, ItemSupportingMaterialHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.components.services.ComponentService
import play.api.libs.json.Json._
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, _ }

import scala.concurrent.Future

object SingleComponent {
  val Key = "singleComponent"
}

class Item(val hooks: CoreItemHooks with CreateItemHook,
  val materialHooks: ItemSupportingMaterialHooks,
  componentService: ComponentService,
  val containerContext: ContainerExecutionContext,
  val playerXhtml: PlayerXhtml,
  val itemInspector: ItemInspector)
  extends CoreItem {

  def componentTypes = componentService.components.map(_.componentType)

  def createWithSingleComponent(componentType: String) = Action.async { implicit request =>

    val defaultData = componentService.interactions
      .find(_.componentType == componentType)
      .map { _.defaultData }
      .flatMap { case o: JsObject => Some(o); case _ => None }

    val collectionId = request.body.asJson.flatMap { j =>
      (j \ "collectionId").asOpt[String]
    }
    defaultData.map { d =>
      hooks.createSingleComponentItem(collectionId, componentType, SingleComponent.Key, d).map { either =>
        either match {
          case Left(sm) => toResult(sm)
          case Right(id) => Created(Json.obj("itemId" -> id))
        }
      }
    }.getOrElse(Future.successful(NotFound(obj("error" -> s"unknown componentType: $componentType"))))
  }

  def create = Action.async { implicit request =>
    val collectionId = request.body.asJson.flatMap { j =>
      (j \ "collectionId").asOpt[String]
    }
    createItem(collectionId)
  }

  private def createItem(collectionId: Option[String])(implicit rh: RequestHeader): Future[SimpleResult] = {

    hooks.createItem(collectionId).map {
      either =>
        either match {
          case Left(sm) => sm
          case Right(id) => Ok(Json.obj("itemId" -> id))
        }
    }
  }

}
