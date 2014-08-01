package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.{ FullSession, SaveSession, SessionOutcome, SessionHooks => ContainerSessionHooks }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2.auth.{ LoadOrgAndOptions, SessionAuth }
import org.corespring.v2.auth.cookies.V2PlayerCookieReader
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.log.V2LoggerFactory
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait SessionHooks
  extends ContainerSessionHooks
  with V2PlayerCookieReader
  with LoadOrgAndOptions {

  def auth: SessionAuth[OrgAndOpts]

  def sessionService: MongoService

  def itemService: ItemService

  def transformItem: Item => JsValue

  lazy val logger = V2LoggerFactory.getLogger("SessionHooks")

  private def isComplete(session: JsValue) = (session \ "isComplete").asOpt[Boolean].getOrElse(false)

  override def load(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] =
    Future(Left(NOT_FOUND -> "Not implemented"))

  override def loadItemAndSession(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, FullSession]] = Future {
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
    logger.trace(s"save $id")

    val out = for {
      identity <- getOrgIdAndOptions(header)
      models <- auth.loadForWrite(id)(identity)
    } yield {
      val (session, _) = models
      SaveSession(session, isSecure(header), isComplete(session), sessionService.save(_, _))
    }
    out.leftMap(s => (BAD_REQUEST -> s.message)).toEither
  }

  private def buildSession[A](id: String, make: (JsValue, JsValue) => A)(implicit header: RequestHeader): Either[StatusMessage, A] = {
    val out = for {
      identity <- getOrgIdAndOptions(header)
      models <- auth.loadForRead(id)(identity)
    } yield {
      val (session, item) = models
      make(transformItem(item), session)
    }
    out.leftMap { s => UNAUTHORIZED -> s.message }.toEither
  }

  private def isSecure(r: RequestHeader) = renderOptions(r).map {
    ro => ro.secure
  }.getOrElse(true)
}

