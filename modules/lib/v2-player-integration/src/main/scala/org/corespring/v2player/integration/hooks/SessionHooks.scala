package org.corespring.v2player.integration.hooks

import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.{ FullSession, SaveSession, SessionOutcome, SessionHooks => ContainerSessionHooks }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2player.integration.auth.SessionAuth
import org.corespring.v2player.integration.cookies.V2PlayerCookieReader
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Validation

trait SessionHooks
  extends ContainerSessionHooks
  with V2PlayerCookieReader {

  def auth: SessionAuth

  def sessionService: MongoService

  def itemService: ItemService

  def transformItem: Item => JsValue

  private def isComplete(session: JsValue) = (session \ "isComplete").asOpt[Boolean].getOrElse(false)

  override def loadEverything(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, FullSession]] = Future {
    buildSession(id, (item, session) =>
      FullSession(Json.obj("item" -> item, "session" -> session), isSecure(header)))
  }

  override def getScore(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, SessionOutcome]] = Future {
    buildSession(id, (item, session) =>
      SessionOutcome(item, session, isSecure(header), isComplete(session)))
  }

  override def loadOutcome(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, SessionOutcome]] = Future {
    buildSession(id, (item, session) =>
      SessionOutcome(item, session, isSecure(header), isComplete(session)))
  }

  override def save(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, SaveSession]] = Future {
    val out = for {
      models <- auth.loadForWrite(id)
    } yield {
      val (session, _) = models
      SaveSession(session, isSecure(header), isComplete(session), sessionService.save(_, _))
    }
    out.leftMap(s => (BAD_REQUEST -> s)).toEither
  }

  private def buildSession[A](id: String, make: (JsValue, JsValue) => A)(implicit header: RequestHeader): Either[StatusMessage, A] = {
    val out: Validation[String, A] = for {
      models <- auth.loadForRead(id)
    } yield {
      val (session, item) = models
      make(transformItem(item), session)
    }
    out.leftMap { s => BAD_REQUEST -> s }.toEither
  }

  private def isSecure(r: RequestHeader) = renderOptions(r).map {
    ro => ro.secure
  }.getOrElse(true)

  override def load(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future(Left(NOT_FOUND -> "Not implemented"))

}
