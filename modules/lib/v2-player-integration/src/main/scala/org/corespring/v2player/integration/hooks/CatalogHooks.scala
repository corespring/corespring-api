package org.corespring.v2player.integration.hooks

import org.corespring.container.client.hooks.{ CatalogHooks => ContainerCatalogHooks }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait CatalogHooks extends ContainerCatalogHooks {

  def itemService: ItemService

  def transform: Item => JsValue

  private lazy val logger = Logger("v2player.catalog.client.actions")

  private def load(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val result: Validation[String, Item] = for {
      oid <- VersionedId(itemId).toSuccess("Invalid object id")
      item <- itemService.findOneById(oid).toSuccess(s"Can't find an item with id: $itemId")
    } yield item

    result match {
      case Success(item) => {
        val pocJson = transform(item)
        Right(pocJson)
      }
      case Failure(message) => Left(BAD_REQUEST -> "Not found: $itemId")
    }
  }

  //TODO: Add auth
  override def showCatalog(itemId: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    logger.debug(s"[editItem] $itemId")
    None
  }

  override def loadItem(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = load(id)
}
