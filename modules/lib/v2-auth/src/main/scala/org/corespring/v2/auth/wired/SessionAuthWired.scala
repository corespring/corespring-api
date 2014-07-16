package org.corespring.v2.auth.wired

import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.v2.auth.{ ItemAuth, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantLoadSession, noItemIdInSession }
import org.corespring.v2.errors.V2Error
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scalaz.Scalaz._
import scalaz.Validation

trait SessionAuthWired extends SessionAuth {

  lazy val logger = LoggerFactory.getLogger("v2.auth.integration.SessionAuth")

  def itemAuth: ItemAuth

  def sessionService: MongoService

  override def loadForRead(sessionId: String)(implicit header: RequestHeader): Validation[V2Error, (JsValue, Item)] = load(sessionId)

  override def loadForWrite(sessionId: String)(implicit header: RequestHeader): Validation[V2Error, (JsValue, Item)] = load(sessionId)

  private def load(sessionId: String)(implicit header: RequestHeader): Validation[V2Error, (JsValue, Item)] = {
    val out = for {
      json <- sessionService.load(sessionId).toSuccess(cantLoadSession(sessionId))
      itemId <- (json \ "itemId").asOpt[String].toSuccess(noItemIdInSession(sessionId))
      item <- itemAuth.loadForRead(itemId)
    } yield (json, item)
    logger.trace(s"loadFor sessionId: $sessionId - result: $out")
    out
  }

  override def canCreate(itemId: String)(implicit header: RequestHeader): Validation[V2Error, Boolean] = {
    itemAuth.loadForRead(itemId).map { i => true }
  }
}
