package org.corespring.v2player.integration.hooks

import org.corespring.container.client.hooks.{ PlayerData, EditorHooks => ContainerEditorHooks }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.auth.ItemAuth
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait EditorHooks extends ContainerEditorHooks {

  import play.api.http.Status._

  def itemService: ItemService

  def transform: Item => JsValue

  def auth: ItemAuth

  private lazy val logger = Logger("v2player.editor.client.actions")

  override def loadItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val result: Validation[String, Item] = for {

      canRead <- auth.canRead(itemId)
      e <- if (canRead) Success(true) else Failure(s"Can't read item with id $itemId")
      oid <- VersionedId(itemId).toSuccess("Invalid object id")
      item <- itemService.findOneById(oid).toSuccess(s"Can't find an item with id: $itemId")
    } yield item

    result
      .rightMap { i => transform(i) }
      .leftMap { s => UNAUTHORIZED -> s }
      .toEither
  }

  override def editItem(itemId: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    logger.debug(s"[editItem] $itemId")
    auth.canWrite(itemId) match {
      case Success(true) => None
      case Success(false) => Some(UNAUTHORIZED -> "not authorized")
      case Failure(msg) => Some((UNAUTHORIZED, msg))
    }
  }

  override def createItem(implicit header: RequestHeader): Future[Either[(Int, String), PlayerData]] = Future(Left(BAD_REQUEST -> "Not Supported"))

}
