package org.corespring.v2player.integration.controllers.editor

import org.corespring.container.client.actions._
import org.corespring.container.client.controllers.hooks.EditorHooks
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait AuthEditorActions {

  def edit(itemId: String)(error: (Int, String) => Future[SimpleResult])(block: Request[AnyContent] => Future[SimpleResult]): Action[AnyContent]
}

trait EditorHooksWithActions extends EditorHooks {

  def itemService: ItemService

  def transform: Item => JsValue

  def auth: AuthEditorActions

  override def actions: EditorActions[AnyContent] = new EditorActions[AnyContent] {

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

    override def editItem(itemId: String)(error: (Int, String) => Future[SimpleResult])(block: (PlayerRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = auth.edit(itemId)(error) {
      request =>

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
          case Failure(message) => error(1111, message)
        }
    }

    def loadComponents(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadServices(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadConfig(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def createSessionForItem(itemId: String)(block: (SessionIdRequest[AnyContent]) => Result): Action[AnyContent] = Action(BadRequest("Not supported"))

    def createItem(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = Action(BadRequest("TODO"))

  }
}
