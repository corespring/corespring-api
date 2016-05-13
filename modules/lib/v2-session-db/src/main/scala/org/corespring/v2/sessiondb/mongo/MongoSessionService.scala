package org.corespring.v2.sessiondb.mongo

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.sessiondb.{SessionReportingUnsupported, SessionService}
import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.joda.time.DateTime
import play.api.libs.json.JsValue

import scalaz.Validation

class MongoSessionService(collection: MongoCollection) extends SessionService with SessionReportingUnsupported {

  val impl = new MongoService(collection)

  def create(data: JsValue): Option[ObjectId] = impl.create(data)

  def load(id: String): Option[JsValue] = {
    val result = impl.load(id)
    result
  }
  def loadMultiple(ids: Seq[String]): Seq[JsValue] = {
    val result = impl.loadMultiple(ids)
    println(s"XXXXXXXXXXXXXxx loadMultiple $ids $result")
    result
  }
  def save(id: String, data: JsValue): Option[JsValue] = impl.save(id, data)

  override def sessionCount(itemId: VersionedId[ObjectId]): Long = {
    val query = MongoDBObject("itemId" -> itemId.toString)
    impl.collection.count(query)
  }



}
