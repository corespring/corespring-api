package org.corespring.platform.core.services

import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import org.specs2.mock.Mockito
import org.specs2.mutable.{Specification}
import play.api.libs.json.Json

class StandardQueryServiceTest extends Specification with Mockito {

  val sut = StandardQueryService

  "getQuery" should {

    "return a dotNotation query if dotNotation is defined" in {
      val query = sut.getQuery(Json.obj("dotNotation" -> "RL.1.1").toString())
      query === Some(MongoDBObject("dotNotation" -> "RL.1.1"))
    }

    "return a standardSearch query if searchTerm is defined" in {
      val query = sut.getQuery(Json.obj("searchTerm" -> "test").toString())
      query === Some(
        MongoDBObject("$or" -> MongoDBList(
          MongoDBObject("standard" -> MongoDBObject("$regex" -> "test", "$options" -> "i")),
          MongoDBObject("subject" -> MongoDBObject("$regex" -> "test", "$options" -> "i")),
          MongoDBObject("category" -> MongoDBObject("$regex" -> "test", "$options" -> "i")),
          MongoDBObject("subCategory" -> MongoDBObject("$regex" -> "test", "$options" -> "i")),
          MongoDBObject("dotNotation" -> MongoDBObject("$regex" -> "test", "$options" -> "i")))))
    }

    "return a standardSearch with filters when filters is defined" in {
      val query = sut.getQuery(
        Json.obj(
          "searchTerm" -> "test",
          "filters" -> Json.obj("category" -> "categoryValue")).toString())
      query === Some(
        MongoDBObject("$or" -> MongoDBList(
          MongoDBObject("standard" -> MongoDBObject("$regex" -> "test" ,"$options" -> "i")),
          MongoDBObject("subject" -> MongoDBObject("$regex" -> "test", "$options" -> "i")),
          MongoDBObject("category" -> MongoDBObject("$regex" -> "test", "$options" -> "i")),
          MongoDBObject("subCategory" -> MongoDBObject("$regex" -> "test", "$options" -> "i")),
          MongoDBObject("dotNotation" -> MongoDBObject("$regex" -> "test", "$options" -> "i"))),
          "category" -> "categoryValue")
      )
    }
  }

}
