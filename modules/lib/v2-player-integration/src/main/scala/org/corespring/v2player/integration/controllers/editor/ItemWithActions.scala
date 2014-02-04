package org.corespring.v2player.integration.controllers.editor

import org.corespring.container.client.actions._
import org.corespring.container.client.controllers.resources.Item
import org.corespring.platform.core.models.item.{ Item => ModelItem }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.JsValue
import play.api.mvc.{ Action, Result, AnyContent }

trait ItemWithActions extends Item {

  def itemService: ItemService

  def transform: ModelItem => JsValue

  override def actions: ItemActions[AnyContent] = new ItemActions[AnyContent] {

    def load(itemId: String)(block: (ItemRequest[AnyContent]) => Result): Action[AnyContent] = Action { request =>
      val item = for {
        id <- VersionedId(itemId)
        item <- itemService.findOneById(id)
      } yield item

      item.map { i =>
        val pocJson = transform(i)
        block(ItemRequest(pocJson, request))
      }.getOrElse(NotFound("?"))
    }

    def save(itemId: String)(block: (SaveItemRequest[AnyContent]) => Result): Action[AnyContent] = Action(BadRequest("Not ready yet"))

    def getScore(itemId: String)(block: (ScoreItemRequest[AnyContent]) => Result): Action[AnyContent] = ???

    override def create(error: (Int, String) => Result)(block: (NewItemRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok(""))
  }
}
