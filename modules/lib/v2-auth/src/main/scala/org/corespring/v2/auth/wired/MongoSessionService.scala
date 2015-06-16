package org.corespring.v2.auth.wired

import com.mongodb.casbah.MongoCollection
import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.v2.auth.services.SessionService
import play.api.libs.json.JsValue

class MongoSessionService(collection:MongoCollection) extends SessionService {

  val impl = new MongoService(collection)

  override def create(data: JsValue): Option[ObjectId] = impl.create(data)

  override def load(id: String): Option[JsValue] = impl.load(id)

  override def save(id: String, data: JsValue): Option[JsValue] = impl.save(id,data)

}
