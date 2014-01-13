package org.corespring.v2player.integration.controllers.editor

import org.corespring.container.client.actions._
import org.corespring.container.client.controllers.resources.Item
import org.corespring.platform.core.models.item.{Item => ModelItem}
import play.api.mvc.{Action, Result, AnyContent}
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.JsValue
import org.corespring.container.client.actions.SaveItemRequest
import org.corespring.container.client.actions.ItemRequest

trait ItemWithBuilder extends Item {

  def itemService : ItemService

  def transform : ModelItem => JsValue

  def builder: ItemActionBuilder[AnyContent] = new ItemActionBuilder[AnyContent] {

    def load(itemId: String)(block: (ItemRequest[AnyContent]) => Result): Action[AnyContent] = Action{ request =>
      val item = for{
        id <- VersionedId(itemId)
        item <- itemService.findOneById(id)
      } yield item

      item.map{ i =>
        val pocJson = transform(i)
        block(ItemRequest(pocJson, request))
      }.getOrElse(NotFound("?"))
    }

    def save(itemId: String)(block: (SaveItemRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))

    def getScore(itemId: String)(block: (ScoreItemRequest[AnyContent]) => Result): Action[AnyContent] = ???
  }
}
