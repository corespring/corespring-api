package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks._
import org.corespring.container.client.hooks.{CatalogHooks => ContainerCatalogHooks}
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.{DisplayConfigJson, OrgAndOpts}
import org.corespring.v2.auth.{ItemAuth, LoadOrgAndOptions}
import org.corespring.v2.errors.V2Error
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait CatalogAssets {
  def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult
}

class CatalogHooks(
  itemService: ItemService,
  transformer: ItemTransformer,
  auth: ItemAuth[OrgAndOpts],
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  catalogAssets: CatalogAssets,
  override implicit val containerContext: ContainerExecutionContext) extends ContainerCatalogHooks with LoadOrgAndOptions {

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {
    val result = for {
      oid <- VersionedId(itemId).toSuccess("Invalid object id")
      identity <- getOrgAndOptions(header)
      item <- auth.loadForRead(itemId)(identity)
    } yield (item, identity)

    result match {
      case Success(result) => {
        val (item, identity) = result
        val pocJson = transformer.transformToV2Json(item)
        Right((pocJson, DisplayConfigJson(identity)))
      }
      case Failure(message) => Left(BAD_REQUEST -> "Not found: $itemId")
    }
  }

  override def showCatalog(itemId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future {
    val result = for {
      identity <- getOrgAndOptions(header)
      item <- auth.loadForRead(itemId)(identity)
    } yield (item, identity)

    result match {
      case Success(itemAndIdentity) => Right(DisplayConfigJson(itemAndIdentity._2))
      case Failure(e) => Left((UNAUTHORIZED, e.message))
    }
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    catalogAssets.loadFile(id, path)(request)
  }
}
