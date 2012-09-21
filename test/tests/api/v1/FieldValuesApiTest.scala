package tests.api.v1

import org.specs2.mutable.Specification
import models.{FieldValue, Item, ItemResponse, ItemSession}
import org.bson.types.ObjectId
import org.specs2.execute.Pending
import play.api.libs.json.Json
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Results, SimpleResult, AnyContentAsJson}
import play.api.test.{FakeHeaders, FakeRequest}
import org.specs2.mutable._
import play.api.test.Helpers._
import tests.PlaySingleton
import com.mongodb.BasicDBObject

object FieldValuesApiTest extends Specification {

  PlaySingleton.start()

   val FieldValueCount =  FieldValue.descriptions.toList.length + 2

  "FieldValuesApi" should {
    "show all available values" in {

      val call = api.v1.routes.FieldValuesApi.getAllAvailable()
      val request = FakeRequest(call.method, call.url )
      val maybeResult = routeAndCall(request)

      if (!maybeResult.isDefined) {
        failure
      }
      val result = maybeResult.get.asInstanceOf[SimpleResult[AnyContentAsJson]]
      result.header.status.mustEqual(OK)
      val json: JsValue = Json.parse(contentAsString(result))
      val array: JsArray = json.asInstanceOf[JsArray]

      array.value.length.mustEqual(FieldValueCount)

      //iterate through each path and ensure its a 200
      for (jso <- array.value) {
        val path = (jso \ "path").asOpt[String]
        val subRequest = FakeRequest(GET, path.get.toString)
        val maybeSubResult = routeAndCall(subRequest)
        if (!maybeSubResult.isDefined) {
          failure
        }

        val subResult = maybeSubResult.get.asInstanceOf[SimpleResult[AnyContentAsJson]]
        subResult.header.status.mustEqual(OK)
      }

      true.mustEqual(true)
    }
  }
}
