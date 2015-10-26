package org.corespring.services.salat.item

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import org.corespring.errors.{ GeneralError, PlatformServiceError }
import org.corespring.models.item.FieldValue
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class FieldValueService(
  val dao: SalatDAO[FieldValue, ObjectId],
  val context: Context) extends interface.item.FieldValueService with HasDao[FieldValue, ObjectId] {

  object Keys {
    val Version = "version"
  }

  override def get = dao.find(MongoDBObject.empty).sort(MongoDBObject("_id" -> -1)).limit(1).toArray.headOption

  override def insert(f: FieldValue): Validation[PlatformServiceError, ObjectId] = {
    dao.insert(f).toSuccess(GeneralError(s"Failed to insert field value: $f", None))
  }

  override def delete(id: ObjectId): Validation[PlatformServiceError, ObjectId] = {
    val result = dao.removeById(id)
    if (!result.getLastError.ok) {
      Failure(GeneralError(s"Failed to delete field value with id $id", None))
    } else {
      Success(id)
    }
  }
}
