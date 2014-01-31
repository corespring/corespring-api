package org.corespring.v2player.integration.controllers.editor

import org.corespring.container.client.actions.{EditorClientHooksActionBuilder, SessionIdRequest, PlayerRequest}
import org.corespring.container.client.controllers.hooks.EditorHooks
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.JsValue
import play.api.mvc.{SimpleResult, Action, Result, AnyContent}
import scalaz.Scalaz._
import scalaz._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

trait EditorHooksWithBuilder extends EditorHooks {

  def itemService: ItemService

  def transform: Item => JsValue

  def builder: EditorClientHooksActionBuilder[AnyContent] = new EditorClientHooksActionBuilder[AnyContent] {

    private lazy val logger = Logger("v2player.editor.client.actions")

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

    override def editItem(itemId: String)(block: (PlayerRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action.async {
      request =>


        import ExecutionContext.Implicits.global

        logger.debug(s"[editItem] $itemId")
        val result = for {
          oid <- VersionedId(itemId).toSuccess("Invalid VersionedId")
          item <- itemService.findOneById(oid).toSuccess(s"Can't find an item with id: $itemId")
        } yield item

        result match {
          case Success(item) => {
            val pocJson = transform(item)
            block(PlayerRequest(pocJson, request))
          }
          case Failure(message) => Future(BadRequest(message))
        }

    }

    def loadComponents(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadServices(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadConfig(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def createSessionForItem(itemId: String)(block: (SessionIdRequest[AnyContent]) => Result): Action[AnyContent] = Action(BadRequest("Not supported"))

    def createItem(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = Action(BadRequest("TODO"))

  }
}
