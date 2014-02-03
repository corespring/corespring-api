package org.corespring.v2player.integration.controllers.editor

import org.corespring.container.client.actions.{ EditorClientHooksActionBuilder, SessionIdRequest, PlayerRequest, ClientHooksActionBuilder }
import org.corespring.container.client.controllers.hooks.EditorHooks
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.{ SimpleResult, Action, Result, AnyContent }
import scalaz.Scalaz._
import scalaz._
import scala.concurrent.Future

trait EditorHooksWithBuilder extends EditorHooks {

  def itemService: ItemService

  def transform: Item => JsValue

  def builder: EditorClientHooksActionBuilder[AnyContent] = new EditorClientHooksActionBuilder[AnyContent] {

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

    def createItem(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = Action(BadRequest("TODO"))

    //TODO: flesh this out
    override def editItem(itemId: String)(block: (PlayerRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action(BadRequest("TODO"))
  }
}
