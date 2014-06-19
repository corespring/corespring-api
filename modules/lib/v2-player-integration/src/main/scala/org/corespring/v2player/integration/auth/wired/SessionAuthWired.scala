package org.corespring.v2player.integration.auth.wired

import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.v2player.integration.auth.{ ItemAuth, SessionAuth }
import org.corespring.v2player.integration.errors.Errors.{ noItemIdInSession, cantLoadSession }
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Validation }
import scalaz.Scalaz._

trait SessionAuthWired extends SessionAuth {

  def itemAuth: ItemAuth

  def sessionService: MongoService

  override def loadForRead(sessionId: String)(implicit header: RequestHeader): Validation[String, (JsValue, Item)] = {
    loadFor(sessionId, itemAuth.loadForRead)
  }

  override def loadForWrite(sessionId: String)(implicit header: RequestHeader): Validation[String, (JsValue, Item)] = {
    loadFor(sessionId, itemAuth.loadForWrite)
  }

  private def loadFor(sessionId: String, loadItem: (String) => Validation[String, Item])(implicit header: RequestHeader): Validation[String, (JsValue, Item)] = {
    for {
      json <- sessionService.load(sessionId).toSuccess(cantLoadSession(sessionId).message)
      itemId <- (json \ "itemId").asOpt[String].toSuccess(noItemIdInSession(sessionId).message)
      item <- loadItem(itemId)
    } yield (json, item)
  }

  override def canCreate(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    itemAuth.loadForRead(itemId).map { i => true }
  }
}
