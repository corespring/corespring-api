package tests.api.v1.item

import tests.BaseTest
import api.v1.item.QueryCleaner
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import models.Organization
import com.mongodb.casbah.query.dsl._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import play.api.libs.json.JsArray

class QueryCleanerTest extends BaseTest {

  lazy val mockOrg: Organization = Organization.findOne(MongoDBObject("$where" -> "this.contentcolls.length > 1")) match {
    case Some(o) => o
    case _ => throw new RuntimeException("something wrong with the test data - need an org with access to more than one collection")
  }

  "query cleaner" should {

    "allow collection ids to be a $in array" in {
      val id = mockOrg.contentcolls(0).collectionId
      val subQuery =
        Some("{ \"collectionId\" : { \"$in\" : [\"" + id.toString + "\"] }}")
      val cleanedQuery = QueryCleaner.clean(subQuery, mockOrg.id)
      val jsValue = play.api.libs.json.Json.parse(cleanedQuery.toString)
      val list: Seq[String] = (jsValue \ "collectionId" \ "$in").as[Seq[String]]
      list.size === 1
    }

    "allow collection ids to be a string" in {
      val id = mockOrg.contentcolls(0).collectionId
      val subQuery =
        Some("{ \"collectionId\" : \"" + id.toString + "\"}")
      val cleanedQuery = QueryCleaner.clean(subQuery, mockOrg.id)
      val jsValue = play.api.libs.json.Json.parse(cleanedQuery.toString)
      val cleanedId: String = (jsValue \ "collectionId" ).as[String]
      id.toString === cleanedId.toString
    }
  }
}
