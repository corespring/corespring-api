package org.corespring.v2player.integration.controllers.player

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{ SessionHooks => ContainerSessionHooks, _ }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.SessionAuth
import org.corespring.v2player.integration.actionBuilders.access.V2PlayerCookieReader
import play.api.http.Status._
import play.api.libs.json.{ JsString, JsValue, Json }
import play.api.mvc._

import scala.concurrent.Future
import scala.language.implicitConversions
import scalaz.Scalaz._
import scalaz._

trait SessionHooks extends ContainerSessionHooks with V2PlayerCookieReader {

  def itemService: ItemService

  def sessionService: MongoService

  def transformItem: Item => JsValue

  def auth: SessionAuth

  def oid(s: String) = if (ObjectId.isValid(s)) Some(new ObjectId(s)) else None

  override def loadEverything(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, FullSession]] = Future {
    val out: Validation[String, FullSession] = for {
      canRead <- auth.read(id)
      itemAndSession <- loadItemAndSession(id)
    } yield {
      val (item, session) = itemAndSession
      FullSession(Json.obj("item" -> item, "session" -> session), isSecure(header))
    }
    out.leftMap { s => HttpStatusMessage(BAD_REQUEST, s) }.toEither
  }

  override def getScore(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, SessionOutcome]] = Future {
    val out = for {
      canRead <- auth.read(id)
      itemAndSession <- loadItemAndSession(id)
    } yield {
      val (item, session) = itemAndSession
      SessionOutcome(item, session, isSecure(header), isComplete(session))
    }
    out.leftMap(s => HttpStatusMessage(BAD_REQUEST, s)).toEither
  }

  override def loadOutcome(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, SessionOutcome]] = Future {
    val out = for {
      canRead <- auth.read(id)
      itemAndSession <- loadItemAndSession(id)
    } yield {
      val (item, session) = itemAndSession
      SessionOutcome(item, session, isSecure(header), isComplete(session))
    }
    out.leftMap(s => HttpStatusMessage(BAD_REQUEST, s)).toEither
  }

  override def save(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, SaveSession]] = Future {
    val out = for {
      canRead <- auth.read(id)
      s <- loadSession(id)
    } yield {
      SaveSession(s, isSecure(header), isComplete(s), sessionService.save(_, _))
    }
    out.leftMap(s => HttpStatusMessage(BAD_REQUEST, s)).toEither
  }

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

  private implicit def handleValidationResult(v: Validation[String, FullSession]) = v match {
    case Failure(err) => Left(HttpStatusMessage(BAD_REQUEST, err))
    case Success(r) => Right(r)
  }

  private def isSecure(r: RequestHeader) = renderOptions(r).map {
    ro => ro.secure
  }.getOrElse(true)

  private def isComplete(session: JsValue) = (session \ "isComplete").asOpt[Boolean].getOrElse(false)

  override def load(id: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, JsValue]] = Future(Left(HttpStatusMessage(NOT_FOUND, "Not implemented")))

}
