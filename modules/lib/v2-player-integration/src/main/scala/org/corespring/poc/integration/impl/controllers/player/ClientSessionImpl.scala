package org.corespring.poc.integration.impl.controllers.player

import org.bson.types.ObjectId
import org.corespring.container.client.actions._
import org.corespring.container.client.controllers.resources.Session
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.{Action, Result, AnyContent}
import scala.Some
import scalaz.Scalaz._
import scalaz._

trait ClientSessionImpl extends Session{

  def itemService: ItemService

  def sessionService: MongoService

  def transformItem: Item => JsValue

  def saveSession(id: String, pocSession: JsValue): Option[JsValue] = sessionService.save(id, pocSession)

  def oid(s: String) = if (ObjectId.isValid(s)) Some(new ObjectId(s)) else None

  private def loadSession(sessionId: String): Validation[String, JsValue] = for {
    oid <- oid(sessionId).toSuccess("invalid object id")
    session <- sessionService.load(oid.toString).toSuccess("can't find session by id")
  } yield {
    session
  }

  private def versionedId(json: JsValue): Option[VersionedId[ObjectId]] = json match {
    case JsString(s) => VersionedId(s)
    case _ => None
  }

  private def loadItemAndSession(sessionId: String): Validation[String, (JsValue, JsValue)] = for {
    session <- loadSession(sessionId)
    vId <- versionedId(session \ "itemId").toSuccess("can't convert to versioned id")
    item <- itemService.findOneById(vId).toSuccess("can't find item by id")
  } yield {
    (transformItem(item), session)
  }

  private def loadEverythingJson(id: String): Validation[String, JsValue] = loadItemAndSession(id) match {
    case Failure(err) => Failure(err)
    case Success(tuple) => Success(Json.obj("item" -> tuple._1, "session" -> tuple._2))
  }

  private def handleValidationResult(v: Validation[String, Result]) = v match {
    case Failure(err) => BadRequest(Json.obj("error" -> JsString(err)))
    case Success(r) => r
  }

  def builder: SessionActionBuilder[AnyContent] = new SessionActionBuilder[AnyContent] {

    def submitAnswers(id: String)(block: (SubmitSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>
        handleValidationResult{
          loadEverythingJson(id).map(json => block(SubmitSessionRequest(json, saveSession, request)))
        }
    }

    def loadEverything(id: String)(block: (FullSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>
        //TODO: Add security
        handleValidationResult(loadEverythingJson(id).map(json => block(FullSessionRequest(json, false, request))))
    }

    def load(id: String)(block: (FullSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))

    def loadOutcome(id: String)(block: (SessionOutcomeRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>
        //TODO: Plugin in secure and complete
        handleValidationResult(loadItemAndSession(id).map(tuple => block(SessionOutcomeRequest(tuple._1, tuple._2, false, false, request))))
    }

    def save(id: String)(block: (SaveSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>

        //TODO: Add security
        handleValidationResult{
          loadSession(id)
            .map(s => SaveSessionRequest(s, false, false, saveSession, request))
            .map(block)
        }

    }
  }
}
