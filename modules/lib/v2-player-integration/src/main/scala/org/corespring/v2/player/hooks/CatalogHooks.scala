package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.{ CatalogHooks => ContainerCatalogHooks }
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ ItemAuth, LoadOrgAndOptions }
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

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
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

}
