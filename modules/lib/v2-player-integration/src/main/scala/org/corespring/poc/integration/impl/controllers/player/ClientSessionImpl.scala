package org.corespring.poc.integration.impl.controllers.player

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{SubmitAnswersRequest, FullSessionRequest, SessionActionBuilder}
import org.corespring.container.client.controllers.resources.Session
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.itemSession.{ItemSession, ItemSessionCompanion}
import org.corespring.platform.core.services.item.ItemService
import org.corespring.poc.integration.impl.transformers.ItemSessionTransformer
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Result, AnyContent}
import scalaz.Scalaz._
import scalaz._

trait ClientSessionImpl extends Session{

  def sessionService: ItemSessionCompanion

  def itemService: ItemService

  def transformItem: Item => JsValue

  def sessionTransformer: ItemSessionTransformer

  def saveSession(id: String, pocSession: JsValue): Option[JsValue] = {

    val session: ItemSession = sessionTransformer.toItemSession(pocSession)
    val result = sessionService.save(session)

    if (result.getLastError.ok) {
      Some(pocSession)
    } else {
      None
    }
  }

  def oid(s: String) = if (ObjectId.isValid(s)) Some(new ObjectId(s)) else None


  private def loadEverythingJson(id: String): Validation[String, JsValue] = for {
    oid <- oid(id).toSuccess("invalid object id")
    session <- sessionService.findOneById(oid).toSuccess("can't find session by id")
    item <- itemService.findOneById(session.itemId).toSuccess("can't find item by id")
  } yield {
    Json.obj(
      "session" -> sessionTransformer.toPocJson(session),
      "item" -> transformItem(item)
    )
  }

  def builder: SessionActionBuilder[AnyContent] = new SessionActionBuilder[AnyContent] {
    def submitAnswers(id: String)(block: (SubmitAnswersRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>

        loadEverythingJson(id) match {
          case Success(everything) => block(SubmitAnswersRequest(everything, saveSession, request))
          case Failure(msg) => BadRequest(msg)
        }

    }

    def loadEverything(id: String)(block: (FullSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>
        loadEverythingJson(id) match {
          case Success(everything) => block(FullSessionRequest(everything, request))
          case Failure(msg) => BadRequest(msg)
        }
    }

    def load(id: String)(block: (FullSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))
  }
}
