package org.corespring.v2player.integration.auth.wired

import org.corespring.mongo.json.services.MongoService
import org.corespring.v2player.integration.auth.{ ItemAuth, SessionAuth }
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Validation }

trait SessionAuthWired extends SessionAuth {

  def itemAuth: ItemAuth

  def sessionService: MongoService

  override def canRead(sessionId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    sessionService.load(sessionId).map { json =>
      itemAuth.canRead((json \ "itemId").as[String])
    }.getOrElse(Failure(s"Can't find session with id: $sessionId"))
  }

  override def canWrite(sessionId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    sessionService.load(sessionId).map { json =>
      itemAuth.canWrite((json \ "itemId").as[String])
    }.getOrElse(Failure(s"Can't find session with id: $sessionId"))
  }

  override def canCreate(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean] = Failure("TODO")
}
