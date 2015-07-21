package org.corespring.services.salat.item

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.item.FieldValue
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }

trait FieldValueService extends interface.item.FieldValueService with HasDao[FieldValue, ObjectId] {

  val CurrentVersion = "0.0.1"

  object Keys {
    val Version = "version"
  }

  override def get = dao.findOne(MongoDBObject(FieldValue.Version -> CurrentVersion))

}
