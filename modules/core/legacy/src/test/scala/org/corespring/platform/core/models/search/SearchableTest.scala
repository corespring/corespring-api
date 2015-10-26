package org.corespring.platform.core.models.search

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.specs2.mutable.Specification

class SearchableTest extends Specification {

  object CollectionExample extends Searchable {
    override protected val searchableFields: Seq[String] = Seq("name")
  }

  "toSearchObj" should {

    "collection example" in {

      val q = s"""{"name" : "my-collection"}"""
      val initSearch = MongoDBObject("_id" -> MongoDBObject("$in" -> Seq("collectionB1", "collectionB2")))
      CollectionExample.toSearchObj(q, Some(initSearch)) match {
        case Left(e) => {
          println(e)
          failure("should have been ok")
        }
        case Right(dbo) => {
          println(com.mongodb.util.JSON.serialize(dbo))
          success
        }
      }
    }
  }
}
