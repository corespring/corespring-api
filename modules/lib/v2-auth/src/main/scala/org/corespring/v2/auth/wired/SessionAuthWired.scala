package org.corespring.v2.auth.wired

import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.v2.auth.models.{ OrgAndOpts, PlayerOptions }
import org.corespring.v2.auth.{ ItemAuth, LoadOrgAndOptions, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantLoadSession, noItemIdInSession }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.JsValue

import scalaz.Scalaz._
import scalaz.Validation

trait SessionAuthWired extends SessionAuth[OrgAndOpts] with LoadOrgAndOptions {

  lazy val logger = V2LoggerFactory.getLogger("auth.SessionAuth")

  def itemAuth: ItemAuth[OrgAndOpts]

  def sessionService: MongoService

  def hasPermissions(itemId: String, sessionId: String, options: PlayerOptions): Validation[V2Error, Boolean]

  override def loadForRead(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, Item)] = load(sessionId)

  override def loadForWrite(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, Item)] = load(sessionId)

  private def load(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, Item)] = {

    logger.debug(s"[load] $sessionId")

    val out = for {
      json <- sessionService.load(sessionId).toSuccess(cantLoadSession(sessionId))
      itemId <- (json \ "itemId").asOpt[String].toSuccess(noItemIdInSession(sessionId))
      item <- itemAuth.loadForRead(itemId)
      hasPerms <- hasPermissions(item.id.toString, sessionId, identity.opts)
    } yield (json, item)
    logger.trace(s"loadFor sessionId: $sessionId - result successful")
    out
  }

  override def canCreate(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Boolean] = {
    itemAuth.loadForRead(itemId).map { i => true }
  }
}
