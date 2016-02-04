package org.corespring.v2.sessiondb

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scalaz.Validation

trait SessionService extends SessionReporting {
  def sessionCount(itemId: VersionedId[ObjectId]): Long
  def create(data: JsValue): Option[ObjectId]
  def load(id: String): Option[JsValue]
  def save(id: String, data: JsValue): Option[JsValue]
}

case class SessionServices(preview: SessionService, main: SessionService)
