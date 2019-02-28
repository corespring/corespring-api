package org.corespring.container.client.controllers.resources

import org.corespring.container.client.HasContainerContext
import org.corespring.container.client.hooks.CollectionHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, Controller }

import scala.concurrent.ExecutionContext

/** Query service for collection objects */
class Collection(
  val containerContext: ContainerExecutionContext,
  val hooks: CollectionHooks) extends Controller with HasContainerContext {

  /**
   * List all collections i do have access to.
   */
  def list(): Action[AnyContent] = Action.async { implicit request =>
    hooks.list().map {
      case Left((code, msg)) => Status(code)(Json.obj("error" -> msg))
      case Right(arr) => Ok(arr)
    }
  }
}
