package org.corespring.v2.player.hooks

import scala.concurrent.Future

import org.corespring.container.client.hooks.{ EditorHooks => ContainerEditorHooks, PlayerData }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.JsValue
import play.api.mvc._
import scalaz.{ Failure, Success }

trait EditorHooks extends ContainerEditorHooks {

  import play.api.http.Status._

  def itemService: ItemService

  def transform: Item => JsValue

  def auth: ItemAuth

  private lazy val logger = V2LoggerFactory.getLogger("editor.hooks")

  /**
   * Attempt to load the item for write. If that fails but reading is possible redirect to the catalog view
   * @param itemId
   * @param header
   * @return
   */
  override def loadItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {

    logger.trace(s"load item: $itemId")

    auth.loadForWrite(itemId) match {
      case Success(item) => Right(transform(item))
      case Failure(e) => {
        logger.trace(s"can't load item: $itemId for writing - try to load for read and if successful return a SEE_OTHER")
        auth.loadForRead(itemId) match {
          case Success(item) => Left(SEE_OTHER -> org.corespring.container.client.controllers.apps.routes.Catalog.showCatalog(itemId).url)
          case Failure(e) => Left(UNAUTHORIZED -> e.message)
        }
      }
    }
  }

  override def createItem(implicit header: RequestHeader): Future[Either[(Int, String), PlayerData]] = Future(Left(BAD_REQUEST -> "Not Supported"))

}
