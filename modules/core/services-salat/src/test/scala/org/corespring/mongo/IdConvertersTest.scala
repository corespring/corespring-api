package org.corespring.mongo

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.Specification

class IdConvertersTest extends Specification with IdConverters {

  "vidToDbo" should {
    "add the id to the dbo" in {
      val vid = VersionedId(ObjectId.get)
      vidToDbo(vid) must_== MongoDBObject("_id._id" -> vid.id)
    }
    "add the version to the dbo if it is set in the vid" in {
      val vid = VersionedId(ObjectId.get, Some(12))
      vidToDbo(vid) must_== MongoDBObject("_id._id" -> vid.id, "_id.version" -> 12)
    }
  }

}
