package org.corespring.v2player.integration.controllers.player

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{ SessionActions => ContainerSessionActions, SaveSessionRequest, SessionOutcomeRequest, FullSessionRequest, SubmitSessionRequest }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.AuthenticatedSessionActions
import org.corespring.v2player.integration.actionBuilders.access.V2PlayerCookieReader
import play.api.libs.json.{ JsString, JsValue, Json }
import play.api.mvc.Results._
import play.api.mvc.{ Request, Action, Result, AnyContent }
import scala.Some
import scala.language.implicitConversions
import scalaz.Scalaz._
import scalaz._

trait SessionActions extends ContainerSessionActions[AnyContent] with V2PlayerCookieReader {

  def itemService: ItemService

  def sessionService: MongoService

  def transformItem: Item => JsValue

  def auth: AuthenticatedSessionActions

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

  private implicit def handleValidationResult(v: Validation[String, Result]) = v match {
    case Failure(err) => BadRequest(Json.obj("error" -> JsString(err)))
    case Success(r) => r
  }

  override def submitAnswers(id: String)(block: (SubmitSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>
      handleValidationResult {
        loadEverythingJson(id).map(json => block(SubmitSessionRequest(json, sessionService.save(_, _), request)))
      }
  }

  private def isSecure(r: Request[AnyContent]) = renderOptions(r).map {
    ro => ro.secure
  }.getOrElse(true)

  private def isComplete(session: JsValue) = (session \ "isComplete").asOpt[Boolean].getOrElse(false)

  override def loadEverything(id: String)(block: (FullSessionRequest[AnyContent]) => Result): Action[AnyContent] = auth.read(id) {
    request =>
      handleValidationResult(loadEverythingJson(id).map(json => block(FullSessionRequest(json, isSecure(request), request))))
  }

  override def load(id: String)(block: (FullSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action(Ok("TODO"))

  override def loadOutcome(id: String)(block: (SessionOutcomeRequest[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>
      loadItemAndSession(id).map {
        tuple =>
          block(SessionOutcomeRequest(tuple._1, tuple._2, isSecure(request), isComplete(tuple._2), request))
      }
  }

  override def save(id: String)(block: (SaveSessionRequest[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>
      loadSession(id)
        .map(s => SaveSessionRequest(s, isSecure(request), isComplete(s), sessionService.save(_, _), request))
        .map(block)

  }

  override def getScore(id: String)(block: (SessionOutcomeRequest[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>

      loadItemAndSession(id)
        .map((t) => {
          val (item, session) = t
          block(SessionOutcomeRequest(item, session, isSecure(request), isComplete(session), request))
        })

  }
}
