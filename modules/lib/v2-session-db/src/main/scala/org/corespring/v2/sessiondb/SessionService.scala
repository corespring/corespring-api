package org.corespring.v2.sessiondb

import org.bson.types.ObjectId
import play.api.libs.json.JsValue

trait SessionService {

  def create(data: JsValue): Option[ObjectId]
  def load(id: String): Option[JsValue]
  def save(id: String, data: JsValue): Option[JsValue]

}

