package org.corespring.api.v1

import org.corespring.platform.core.models.item.FieldValue
import org.corespring.test.BaseTest
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some

object FieldValuesApiTest extends BaseTest {

   val FieldValueCount =  FieldValue.descriptions.toList.length + 2

   val routes = org.corespring.api.v1.routes.FieldValuesApi

  "FieldValuesApi" should {
    "show all available values" in {

      val call = routes.getAllAvailable()
      val request = FakeRequest(call.method, call.url )
      val maybeResult = route(request)

      if (!maybeResult.isDefined) {
        failure
      }
      val result = maybeResult.get
      status(result) === OK
      val json: JsValue = Json.parse(contentAsString(result))
      val array: JsArray = json.asInstanceOf[JsArray]

      array.value.length.mustEqual(FieldValueCount)

      //iterate through each path and ensure its a 200
      for (jso <- array.value) {
        val path = (jso \ "path").asOpt[String]
        val subRequest = FakeRequest(GET, path.get.toString)
        val maybeSubResult = route(subRequest)
        if (!maybeSubResult.isDefined) {
          failure
        }

        val subResult = maybeSubResult.get
        status(subResult) === OK
      }

      true.mustEqual(true)
    }

    "return multiple values" in {

      val call = routes.multiple("gradeLevels,reviewsPassed")
      val request = FakeRequest(call.method, call.url)

      route(request) match {
        case Some(result) => {
          val json : JsValue = Json.parse(contentAsString(result))
          ((json \ "gradeLevels").asOpt[List[JsObject]].getOrElse(List()).length > 0)  === true
          ((json \ "reviewsPassed").asOpt[List[JsObject]].getOrElse(List()).length > 0) === true
        }
        case _ => failure("call failed")
      }
    }

    "return multiple values with queries" in {
      val options = """{"subject" : { "q" : {"category": "Art"}} }"""
      val call = routes.multiple("subject,reviewsPassed", Some(options) )
      val request = FakeRequest(call.method, call.url)

      route(request) match {
       case Some(result) => {
          val json = Json.parse(contentAsString(result))
          (((json\ "subject")).asOpt[List[JsObject]].getOrElse(List()).length > 0) === true
       }
       case _ => failure("request unsuccessful")
      }
    }

    "multiple handles invalid json" in {
      val call = routes.multiple("reviewsPassed", Some("asdfadsf") )
      val request = FakeRequest(call.method, call.url)

      route(request) match {
       case Some(result) => {
          val json = Json.parse(contentAsString(result))
          (((json\ "reviewsPassed")).asOpt[List[JsObject]].getOrElse(List()).length > 0) === true
       }
       case _ => failure("request unsuccessful")
      }

    }

  }
}
