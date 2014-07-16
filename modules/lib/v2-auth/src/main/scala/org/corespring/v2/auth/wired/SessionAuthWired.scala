package org.corespring.v2.auth.wired

import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.auth.{ LoadOrgAndOptions, ItemAuth, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantLoadSession, noItemIdInSession }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scalaz.Scalaz._
import scalaz.Validation

trait SessionAuthWired extends SessionAuth with LoadOrgAndOptions {

  lazy val logger = V2LoggerFactory.getLogger("auth.SessionAuth")

  def itemAuth: ItemAuth

  def sessionService: MongoService

  def hasPermissions(itemId: String, sessionId: String, options: PlayerOptions): Validation[V2Error, Boolean]

  override def loadForRead(sessionId: String)(implicit header: RequestHeader): Validation[V2Error, (JsValue, Item)] = {
    loadFor(sessionId)
  }

  override def loadForWrite(sessionId: String)(implicit header: RequestHeader): Validation[V2Error, (JsValue, Item)] = {
    loadFor(sessionId)
  }

  private def loadFor(sessionId: String)(implicit header: RequestHeader): Validation[V2Error, (JsValue, Item)] = {

    logger.trace(s"loadFor $sessionId")

    val out = for {
      orgAndOpts <- getOrgIdAndOptions(header)
      json <- sessionService.load(sessionId).toSuccess(cantLoadSession(sessionId))
      itemId <- (json \ "itemId").asOpt[String].toSuccess(noItemIdInSession(sessionId))
      item <- itemAuth.loadForRead(itemId)
      hasPerms <- hasPermissions(item.id.toString, sessionId, orgAndOpts._2)
    } yield (json, item)
    logger.trace(s"loadFor sessionId: $sessionId - result successful")
    out
  }

  override def canCreate(itemId: String)(implicit header: RequestHeader): Validation[V2Error, Boolean] = {
    itemAuth.loadForRead(itemId).map { i => true }
  }
}
