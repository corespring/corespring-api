package org.corespring.container.client.controllers.resources

import org.corespring.container.client.HasContainerContext
import org.corespring.container.client.hooks.{ ItemMetadataHooks, CollectionHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, Controller }

/** Query service for item metadata */
class ItemMetadata(
  val containerContext: ContainerExecutionContext,
  val hooks: ItemMetadataHooks) extends Controller with HasContainerContext {

  /**
   * Get metadata for an item
   */
  def get(id: String): Action[AnyContent] = Action.async { implicit request =>
    hooks.get(id).map {
      case Left((code, msg)) => Status(code)(Json.obj("error" -> msg))
      case Right(data) => Ok(data)
    }
  }
}
