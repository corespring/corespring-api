import api.ApiError
import com.mongodb.{BasicDBObject, DBObject}
import models.{Resource, Item}
import play.api.libs.json.{JsObject, Json, JsValue}
import play.api.mvc.SimpleResult
import play.api.Play
import play.api.Play.current
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.core.Router

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

  def baseItemPath(itemId: String) = {
    "/api/v1/items/" + itemId
  }

  def testItem: Item = {
    val query: DBObject = new BasicDBObject()
    query.put("title", "NOT SURE THIS IS A GOOD ITEM? NO CONNECTION BETWEEN ITEM AND CHART?")
    Item.findOne(query) match {
      case Some(item) => item
      case _ => throw new RuntimeException("test item")
    }
  }


  "resource api" should {


    running(FakeApplication()) {


      "list an item's supporting materials" in {
        val url = baseItemPath(testItem.id.toString) + "/materials"
        routeAndCall(FakeRequest(GET, url)) match {
          case Some(result) => {
            val json: JsValue = Json.parse(contentAsString(result.asInstanceOf[SimpleResult[JsValue]]))
            val seq: Seq[JsObject] = json.as[Seq[JsObject]]
            seq.length must equalTo(1)
            val jsItem = seq(0)
            (jsItem \ "name").asOpt[String] must equalTo(Some("Rubric"))
          }
          case _ => throw new RuntimeException("call failed")
        }
      }

      "do a binary post to supporting materials Resource" in {

        val item = testItem
        val filename = "cute-rabbit.jpg"
        val url = baseItemPath(item.id.toString) + "/materials/Rubric/" + filename + "/upload"
        val file = Play.getFile("test/files/" + filename)
        val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
        val byteArray = source.map(_.toByte).toArray
        source.close()

        //First upload should work
        val firstCall = call(byteArray, url, OK, filename)
        firstCall must equalTo((true, true))

        //subsequent should file beacuse the filename is taken
        val secondCall = call(byteArray, url, NOT_FOUND, ApiError.FilenameTaken.message)
        secondCall must equalTo((true, true))

        val badUrl = baseItemPath(item.id.toString) + "/materials/badResourceName/" + filename + "/upload"

        val thirdCall = call(byteArray, badUrl, NOT_FOUND, ApiError.ResourceNotFound.message)
        thirdCall must equalTo((true, true))
      }

      "do a binary post to Item.data resource" in {

        val item = testItem
        val filename = "cute-rabbit.jpg"
        val url = baseItemPath(item.id.toString) + "/resource/" + filename + "/upload"
        val file = Play.getFile("test/files/" + filename)
        val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
        val byteArray = source.map(_.toByte).toArray
        source.close()

        //First upload should work
        val firstCall = call(byteArray, url, OK, filename)
        firstCall must equalTo((true, true))

        //subsequent should file beacuse the filename is taken
        val secondCall = call(byteArray, url, NOT_FOUND, ApiError.FilenameTaken.message)
        secondCall must equalTo((true, true))
      }
    }
  }
}

