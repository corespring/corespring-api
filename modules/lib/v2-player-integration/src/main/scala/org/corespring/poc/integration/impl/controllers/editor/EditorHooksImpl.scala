package org.corespring.poc.integration.impl.controllers.editor

import org.corespring.container.client.actions.{SessionIdRequest, PlayerRequest, ClientHooksActionBuilder}
import org.corespring.container.client.controllers.hooks.EditorHooks
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Result, AnyContent}
import scalaz.Scalaz._
import scalaz._

trait EditorHooksImpl extends EditorHooks {

  def itemService: ItemService

  def transform: Item => JsValue

  def builder: ClientHooksActionBuilder[AnyContent] = new ClientHooksActionBuilder[AnyContent] {

    private def load(itemId: String)(block: PlayerRequest[AnyContent] => Result) = Action {
      request =>
        val result: Validation[String, Item] = for {
          oid <- VersionedId(itemId).toSuccess("Invalid object id")
          item <- itemService.findOneById(oid).toSuccess(s"Can't find an item with id: $itemId")
        } yield item

        result match {
          case Success(item) => {
            val pocJson = transform(item)
            block(PlayerRequest(pocJson, request))
          }
          case Failure(message) => BadRequest(s"Not found: $itemId")
        }
    }

    def loadComponents(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadServices(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadConfig(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def createSessionForItem(itemId: String)(block: (SessionIdRequest[AnyContent]) => Result): Action[AnyContent] = Action(BadRequest("Not supported"))
}
}
