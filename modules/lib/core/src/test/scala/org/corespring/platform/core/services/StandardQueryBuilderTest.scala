package org.corespring.platform.core.services

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{ BasicDBObject, BasicDBList }
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class StandardQueryBuilderTest extends Specification {

  class queryScope extends StandardQueryBuilder with Scope {

    def assert(in: String, expected: String) = {
      val dbo = getStandardBySearchQuery(in).get
      com.mongodb.util.JSON.serialize(dbo) === expected
    }
  }

  "getStandardBySearchQuery" should {

    "return filters in the dbo" in new queryScope {
      val dbo = getStandardBySearchQuery("""{"filters" : {"subject" : "ELA", "foo" : "bar"}}""").get
      dbo.get("subject") === "ELA"
      dbo.get("foo") === "bar"
    }

    "return $or for searchTerm in the dbo" in new queryScope {
      val dbo = getStandardBySearchQuery("""{"searchTerm" : "a"}""").get
      val list = dbo.get("$or").asInstanceOf[List[BasicDBObject]]

      forall(list) { (dbo) =>
        val value = dbo.values().toArray().apply(0)
        value === MongoDBObject("$regex" -> "a", "$options" -> "i")
      }

      forall(list) { (dbo) =>
        val key = dbo.keySet().toArray().apply(0)
        searchTermKeys.contains(key)
      }
    }

    "returns both" in new queryScope {
      val dbo = getStandardBySearchQuery("""{"searchTerm" : "a", "filters" : {"subject" : "ELA", "foo" : "bar"}}""").get
      dbo.get("$or").asInstanceOf[List[BasicDBObject]] !== null
      dbo.get("subject") === "ELA"
    }
  }
}
