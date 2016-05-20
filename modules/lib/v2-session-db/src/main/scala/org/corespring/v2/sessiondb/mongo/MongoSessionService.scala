package org.corespring.v2.sessiondb.mongo

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.sessiondb.{ SessionReportingUnsupported, SessionService, SessionServiceExecutionContext }
import play.api.libs.json.JsValue

import scala.concurrent.Future

class MongoSessionService(collection: MongoCollection, context: SessionServiceExecutionContext) extends SessionService with SessionReportingUnsupported {

  val impl = new MongoService(collection)

  def create(data: JsValue): Option[ObjectId] = impl.create(data)

  def load(id: String): Option[JsValue] = {
    val result = impl.load(id)
    result
  }

  def loadMultiple(ids: Seq[String]): Future[Seq[(String, Option[JsValue])]] = Future {
    val result = impl.loadMultiple(ids)

    val found = result.map { json =>
      (json \ "_id" \ "$oid").as[String] -> Some(json)
    }

    val notFound = ids.diff(found.map(_._1))
    found ++ notFound.map { id => id -> None }
  }(context.ec)

  def save(id: String, data: JsValue): Option[JsValue] = impl.save(id, data)

  override def sessionCount(itemId: VersionedId[ObjectId]): Long = {
    val query = MongoDBObject("itemId" -> itemId.toString)
    impl.collection.count(query)
  }

}
