package org.corespring.v2.sessiondb

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.JsValue

import scala.concurrent.{ ExecutionContext, Future }

case class SessionServiceExecutionContext(ec: ExecutionContext)

trait SessionService extends SessionReporting {
  def sessionCount(itemId: VersionedId[ObjectId]): Long
  def load(id: String): Option[JsValue]
  def loadMultiple(ids: Seq[String]): Future[Seq[(String, Option[JsValue])]]
  def save(id: String, data: JsValue): Option[JsValue]
  def create(data: JsValue): Option[ObjectId]
}

case class SessionServices(preview: SessionService, main: SessionService)
