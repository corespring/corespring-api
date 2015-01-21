package org.corespring.platform.core.services

import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class SubjectQueryServiceTest extends Specification with Mockito {

  val sut = SubjectQueryService

  "getQuery" should {

    "return a search query if searchTerm is defined" in {
      val query = sut.getQuery(Json.obj("searchTerm" -> "test").toString())
      query === Some(
        MongoDBObject("$or" -> MongoDBList(
          MongoDBObject("subject" -> MongoDBObject("$regex" -> "test", "$options" -> "i")),
          MongoDBObject("category" -> MongoDBObject("$regex" -> "test", "$options" -> "i"))
        )))
    }

    "return a filter query if category and subject are defined" in {
      val query = sut.getQuery(Json.obj("filters" -> Json.obj("category" -> "cat", "subject" -> "sub")).toString())
      query === Some(MongoDBObject(
        "subject" -> "sub",
        "category" -> "cat"))
    }

    "return a filter query if category is defined" in {
      val query = sut.getQuery(Json.obj("filters" -> Json.obj("category" -> "cat")).toString())
      query === Some(MongoDBObject(
        "category" -> "cat"))
    }

  }

}
