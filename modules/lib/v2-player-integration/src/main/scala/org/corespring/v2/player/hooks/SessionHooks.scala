package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.{ FullSession, SaveSession, SessionOutcome, SessionHooks => ContainerSessionHooks }
import org.corespring.platform.core.models.item.{ PlayerDefinition, Item }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ LoadOrgAndOptions, SessionAuth }
import org.corespring.v2.log.V2LoggerFactory
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait SessionHooks
  extends ContainerSessionHooks
  with LoadOrgAndOptions {

  def auth: SessionAuth[OrgAndOpts, PlayerDefinition]

  def itemService: ItemService

  def transformItem: Item => JsValue

  lazy val logger = V2LoggerFactory.getLogger("SessionHooks")

  private def isComplete(session: JsValue) = (session \ "isComplete").asOpt[Boolean].getOrElse(false)

  override def load(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] =
    Future(Left(NOT_FOUND -> "Not implemented"))

  override def loadItemAndSession(id: String)(implicit header: RequestHeader): Either[StatusMessage, FullSession] =
    buildSession(id, (item, session, orgAndOpts) => FullSession(Json.obj("item" -> item, "session" -> session),
      orgAndOpts.opts.secure))

  override def getScore(id: String)(implicit header: RequestHeader): Either[StatusMessage, SessionOutcome] =
    buildSession(id, (item, session, orgAndOpts) =>
      SessionOutcome(item, session, orgAndOpts.opts.secure, isComplete(session)))

  override def loadOutcome(id: String)(implicit header: RequestHeader): Either[StatusMessage, SessionOutcome] =
    buildSession(id, (item, session, orgAndOpts) =>
      SessionOutcome(item, session, orgAndOpts.opts.secure, isComplete(session)))

  override def save(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, SaveSession]] = Future {
    logger.trace(s"save $id")

    val out = for {
      identity <- getOrgAndOptions(header)
      models <- auth.loadForWrite(id)(identity)
      saveFn <- auth.saveSession(identity)
    } yield {
      val (session, _) = models
      SaveSession(session, identity.opts.secure, isComplete(session), saveFn)
    }
    out.leftMap(s => (BAD_REQUEST -> s.message)).toEither
  }

  private def buildSession[A](id: String, make: (JsValue, JsValue, OrgAndOpts) => A)(implicit header: RequestHeader): Either[StatusMessage, A] = {
    val out = for {
      identity <- getOrgAndOptions(header)
      models <- auth.loadForRead(id)(identity)
    } yield {
      val (session, playerDefinition) = models
      logger.trace(s"[buildSession] org and opts: $identity")
      logger.trace(s"[buildSession] playerDefinition: ${playerDefinition}")
      make(Json.toJson(playerDefinition), session, identity)
    }
    out.leftMap { s => UNAUTHORIZED -> s.message }.toEither
  }
}

