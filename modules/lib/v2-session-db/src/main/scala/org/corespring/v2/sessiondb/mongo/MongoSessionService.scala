package org.corespring.v2.sessiondb.mongo

import com.mongodb.casbah.MongoCollection
import org.corespring.v2.sessiondb.SessionService
import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import play.api.libs.json.JsValue

class MongoSessionService(collection: MongoCollection) extends SessionService {

  val impl = new MongoService(collection)

  def create(data: JsValue): Option[ObjectId] = impl.create(data)
  def load(id: String): Option[JsValue] = impl.load(id)
  def save(id: String, data: JsValue): Option[JsValue] = impl.save(id, data)

}
