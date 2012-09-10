import akka.dispatch.{Future}
import api.ApiError
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{BasicDBObject, DBObject}
import models.Item
import org.specs2.mutable.Specification
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.json.{JsValue, JsObject}
import play.api.libs.ws.{Response, WS}
import play.api.mvc.SimpleResult
import play.api.Play
import play.api.Play.current
import play.api.test.TestServer
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

object ResourceApiTest extends Specification {

  def call(byteArray: Array[Byte], url: String, expectedStatus: Int, expectedContains: String): (Boolean, Boolean) = {

    routeAndCall(
      FakeRequest(
        POST,
        url,
        FakeHeaders(Map("Content" -> List("application/octet-stream"))),
        byteArray)) match {
      case Some(result) => {
        val simpleResult = result.asInstanceOf[SimpleResult[JsValue]]
        val stringResult = contentAsString(simpleResult)
        (status(result) == expectedStatus, stringResult.contains(expectedContains))
      }
      case _ => {
        throw new RuntimeException("Error with call")
      }
    }
  }

  "resource api" should {
    "do a binary post to supporting materials Resource" in {

      running(FakeApplication()) {

        val query: DBObject = new BasicDBObject()
        query.put("title", "NOT SURE THIS IS A GOOD ITEM? NO CONNECTION BETWEEN ITEM AND CHART?")
        Item.findOne(query) match {
          case Some(item) => {
            item.id.toString

            println("itemId: " + item.id.toString)
            val filename = "cute-rabbit.jpg"
            val url = "/api/v1/items/" + item.id.toString + "/materials/Rubric/" + filename + "/upload"
            val file = Play.getFile("test/files/" + filename)
            val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
            val byteArray = source.map(_.toByte).toArray
            source.close()

            //First upload should work
            val firstCall = call(byteArray, url, OK, filename)
            firstCall must equalTo((true,true))

            //subsequent should file beacuse the filename is taken
            val secondCall = call(byteArray, url, NOT_FOUND, ApiError.FilenameTaken.message)
            secondCall must equalTo((true,true))

            val badUrl = "/api/v1/items/" + item.id.toString + "/materials/badResourceName/" + filename + "/upload"

            val thirdCall = call(byteArray, badUrl, NOT_FOUND, ApiError.ResourceNotFound.message)
            thirdCall must equalTo((true,true))

          }
          case _ => throw new RuntimeException("can't find upload test item.")
        }
      }
    }

    "do a binary post to Item.data resource" in {

      running(FakeApplication()) {

        val query: DBObject = new BasicDBObject()
        query.put("title", "NOT SURE THIS IS A GOOD ITEM? NO CONNECTION BETWEEN ITEM AND CHART?")
        Item.findOne(query) match {
          case Some(item) => {
            item.id.toString

            println("itemId: " + item.id.toString)
            val filename = "cute-rabbit.jpg"
            val url = "/api/v1/items/" + item.id.toString + "/resource/" + filename + "/upload"
            val file = Play.getFile("test/files/" + filename)
            val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
            val byteArray = source.map(_.toByte).toArray
            source.close()

            //First upload should work
            val firstCall = call(byteArray, url, OK, filename)
            firstCall must equalTo((true,true))

            //subsequent should file beacuse the filename is taken
            val secondCall = call(byteArray, url, NOT_FOUND, ApiError.FilenameTaken.message)
            secondCall must equalTo((true,true))
          }
          case _ => throw new RuntimeException("can't find upload test item.")
        }
      }
    }
  }
}
