package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.{ PlayerData, EditorHooks => ContainerEditorHooks }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.log.V2LoggerFactory
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._

trait EditorHooks extends ContainerEditorHooks {

  import play.api.http.Status._

  def itemService: ItemService

  def transform: Item => JsValue

  def auth: ItemAuth

  private lazy val logger = V2LoggerFactory.getLogger("EditorHooks")

  override def loadItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val result = for {
      item <- auth.loadForRead(itemId)
    } yield item

    result
      .rightMap { i => transform(i) }
      .leftMap { s => UNAUTHORIZED -> s.message }
      .toEither
  }

  override def createItem(implicit header: RequestHeader): Future[Either[(Int, String), PlayerData]] = Future(Left(BAD_REQUEST -> "Not Supported"))

}
