package org.corespring.mongo

import com.mongodb.casbah.Imports._
import org.corespring.platform.data.mongo.models.VersionedId

trait IdConverters {
  def vidToDbo(vid: VersionedId[ObjectId]): DBObject = {
    val base = MongoDBObject("_id._id" -> vid.id)
    vid.version.map { v =>
      base ++ MongoDBObject("_id.version" -> v)
    }.getOrElse(base)
  }
}
