package org.corespring.v2player.integration.hooks

import scala.concurrent.Future

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{ FullSession, HttpStatusMessage, SaveSession, SessionHooks => ContainerSessionHooks, SessionOutcome }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.auth.SessionAuth
import org.corespring.v2player.integration.cookies.V2PlayerCookieReader
import play.api.http.Status._
import play.api.libs.json.{ JsString, JsValue, Json }
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

trait SessionHooks
  extends ContainerSessionHooks
  with V2PlayerCookieReader {

  def auth: SessionAuth

  def sessionService: MongoService

  def itemService: ItemService

  def transformItem: Item => JsValue

  private def isComplete(session: JsValue) = (session \ "isComplete").asOpt[Boolean].getOrElse(false)

  override def loadEverything(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, FullSession]] = Future {
    buildSession(id, (item, session) =>
      FullSession(Json.obj("item" -> item, "session" -> session), isSecure(header)))
  }

  override def getScore(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, SessionOutcome]] = Future {
    buildSession(id, (item, session) =>
      SessionOutcome(item, session, isSecure(header), isComplete(session)))
  }

  override def loadOutcome(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, SessionOutcome]] = Future {
    buildSession(id, (item, session) =>
      SessionOutcome(item, session, isSecure(header), isComplete(session)))
  }

  override def save(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, SaveSession]] = Future {
    val out = for {
      canWrite <- auth.canWriteToSession(id)
      w <- if (canWrite) Success(true) else Failure("No write access")
      s <- loadSession(id)
    } yield {
      SaveSession(s, isSecure(header), isComplete(s), sessionService.save(_, _))
    }
    out.leftMap(s => HttpStatusMessage(BAD_REQUEST, s)).toEither
  }

  private def buildSession[A](id: String, make: (JsValue, JsValue) => A): Either[HttpStatusMessage, A] = {
    val out: Validation[String, A] = for {
      canAccess <- auth.canAccessSession(id)
      allowed <- if (canAccess) Success(true) else Failure("No access")
      itemAndSession <- loadItemAndSession(id)
    } yield {
      val (item, session) = itemAndSession
      make(item, session)
    }
    out.leftMap { s => HttpStatusMessage(BAD_REQUEST, s) }.toEither
  }

  private def isSecure(r: RequestHeader) = renderOptions(r).map {
    ro => ro.secure
  }.getOrElse(true)

  private def versionedId(json: JsValue): Option[VersionedId[ObjectId]] = json match {
    case JsString(s) => VersionedId(s)
    case _ => None
  }

  def oid(s: String) = if (ObjectId.isValid(s)) Some(new ObjectId(s)) else None

  private def loadSession(sessionId: String): Validation[String, JsValue] = for {
    oid <- oid(sessionId).toSuccess("invalid object id")
    session <- sessionService.load(oid.toString).toSuccess("can't find session by id")
  } yield {
    session
  }

  private def loadItemAndSession(sessionId: String): Validation[String, (JsValue, JsValue)] = for {
    session <- loadSession(sessionId)
    vId <- versionedId(session \ "itemId").toSuccess("can't convert to versioned id")
    item <- itemService.findOneById(vId).toSuccess("can't find item by id")
  } yield {
    (transformItem(item), session)
  }

  override def load(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, JsValue]] = Future(Left(HttpStatusMessage(NOT_FOUND, "Not implemented")))

}
