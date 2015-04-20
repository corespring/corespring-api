package org.corespring.v2.player.hooks

import org.corespring.v2.auth.models.OrgAndOpts

import org.corespring.container.client.hooks.{ CatalogHooks => ContainerCatalogHooks }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.{LoadOrgAndOptions, ItemAuth}
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.log.V2LoggerFactory
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait CatalogHooks extends ContainerCatalogHooks with LoadOrgAndOptions {

  def itemService: ItemService

  def transform: Item => JsValue

  def auth: ItemAuth[OrgAndOpts]

  private lazy val logger = V2LoggerFactory.getLogger("Catalog")

  private def load(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val result = for {
      oid <- VersionedId(itemId).toSuccess("Invalid object id")
      identity <- getOrgAndOptions(header)
      item <- auth.loadForRead(itemId)(identity)
    } yield item

    result match {
      case Success(item) => {
        val pocJson = transform(item)
        Right(pocJson)
      }
      case Failure(message) => Left(BAD_REQUEST -> "Not found: $itemId")
    }
  }

  override def showCatalog(itemId: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    val result = for {
      identity <- getOrgAndOptions(header)
      item <- auth.loadForRead(itemId)(identity)
    } yield item

    result match {
      case Success(item) => None
      case Failure(e) => Some((UNAUTHORIZED, e.message))
    }

  }

  override def loadItem(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = load(id)

}
