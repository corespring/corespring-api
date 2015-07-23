package org.corespring.services.salat.item

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import org.corespring.models.item.FieldValue
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }

class FieldValueService(
    val dao : SalatDAO[FieldValue,ObjectId],
    val context : Context
) extends interface.item.FieldValueService with HasDao[FieldValue, ObjectId] {

  val CurrentVersion = "0.0.1"

  object Keys {
    val Version = "version"
  }

  override def get = dao.findOne(MongoDBObject(FieldValue.Version -> CurrentVersion))

}
