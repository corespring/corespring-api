package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.{ CatalogHooks => ContainerCatalogHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ ItemAuth, LoadOrgAndOptions }
import org.corespring.v2.errors.V2Error
import play.api.http.Status._
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait CatalogAssets {
  def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult
  def loadSupportingMaterialFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult
}

class CatalogHooks(
  itemService: ItemService,
  transformer: ItemTransformer,
  auth: ItemAuth[OrgAndOpts],
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  catalogAssets: CatalogAssets,
  override implicit val ec: ContainerExecutionContext) extends ContainerCatalogHooks with LoadOrgAndOptions {

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val result = for {
      oid <- VersionedId(itemId).toSuccess("Invalid object id")
      identity <- getOrgAndOptions(header)
      item <- auth.loadForRead(itemId)(identity)
    } yield item

    result match {
      case Success(item) => {
        val pocJson = transformer.transformToV2Json(item)
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

  override def loadSupportingMaterialFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    catalogAssets.loadSupportingMaterialFile(id, path)(request)
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    catalogAssets.loadFile(id, path)(request)
  }
}
