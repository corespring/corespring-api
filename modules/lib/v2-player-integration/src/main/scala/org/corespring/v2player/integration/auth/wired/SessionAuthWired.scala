package org.corespring.v2player.integration.auth.wired

import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.v2player.integration.auth.{ ItemAuth, SessionAuth }
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Validation }

trait SessionAuthWired extends SessionAuth {

  def itemAuth: ItemAuth

  def sessionService: MongoService

  override def loadForRead(sessionId: String)(implicit header: RequestHeader): Validation[String, (JsValue, Item)] = {
    sessionService.load(sessionId).map { json =>
      val v: Validation[String, Item] = itemAuth.loadForRead((json \ "itemId").as[String])
      v.map(i => (json, i))
    }.getOrElse(Failure(s"Can't find session with id: $sessionId"))
  }

  override def loadForWrite(sessionId: String)(implicit header: RequestHeader): Validation[String, (JsValue, Item)] = {
    sessionService.load(sessionId).map { json =>
      val v: Validation[String, Item] = itemAuth.loadForWrite((json \ "itemId").as[String])
      v.map(i => (json, i))
    }.getOrElse(Failure(s"Can't find session with id: $sessionId"))
  }

  override def canCreate(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean] = Failure("TODO")
}
