import akka.dispatch.{Future}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{BasicDBObject, DBObject}
import models.Item
import org.specs2.mutable.Specification
import play.api.libs.concurrent.Promise
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

  "resource api" should {
    "do a binary post" in {

      running(FakeApplication()) {

        val query: DBObject = new BasicDBObject()
        query.put("title", "NOT SURE THIS IS A GOOD ITEM? NO CONNECTION BETWEEN ITEM AND CHART?")
        Item.findOne(query) match {
          case Some(item) => {
            item.id.toString
            val filename = "cute-rabbit.jpg"
            val url = "/api/v1/items/" + item.id.toString + "/resource/" + filename + "/upload"
            val file = Play.getFile("test/files/" + filename)
            val source = scala.io.Source.fromFile(file.getAbsolutePath)(scala.io.Codec.ISO8859)
            val byteArray = source.map(_.toByte).toArray
            source.close()

            routeAndCall(
              FakeRequest(
                POST,
                url,
                FakeHeaders(Map("Content" -> List("application/octet-stream"))),
                byteArray)) match {
              case Some(result) => {
                println("result:")
                println(result)
                val simpleResult = result.asInstanceOf[SimpleResult[Any]]
                //work in progress
                //println(simpleResult.body.asInstanceOf[JsValue])
                //val actualResult = result.apply(FakeRequest())
                //status(actualResult) must equalTo(OK)
              }
              case _ => {
                throw new RuntimeException("Error with call")
              }
            }

            true mustEqual true
          }
          case _ => throw new RuntimeException("can't find upload test item.")
        }
      }
    }
  }
}
