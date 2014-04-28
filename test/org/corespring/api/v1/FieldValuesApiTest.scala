package org.corespring.api.v1

import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.corespring.platform.core.models.item.FieldValue
import org.corespring.test.BaseTest
import play.api.test.FakeHeaders
import play.api.libs.json._
import scala.Some
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object FieldValuesApiTest extends BaseTest {

  val FieldValueCount =  FieldValue.descriptions.toList.length + 2
  val routes = org.corespring.api.v1.routes.FieldValuesApi

  "FieldValuesApi" should {

    "show all available values" in {

      val result = FieldValuesApi.getAllAvailable()(FakeRequest())
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

    "show field values for current user collections" in {
      val request = FakeRequest(GET, tokenize("/"), FakeHeaders(), AnyContentAsEmpty)
      val result = FieldValuesApi.getFieldValuesAction()(request)
      status(result) must be equalTo OK
      contentAsJson(result) match {
        case json: JsObject => {
          (json \ "contributor").as[JsArray].value must not beEmpty;
          (json \ "subject").as[JsArray].value must not beEmpty;
          (json \ "standard").as[JsArray].value must not beEmpty;
          (json \ "gradeLevel").as[JsArray].value must not beEmpty;
          (json \ "keySkill").as[JsArray].value must not beEmpty;
          (json \ "itemType").as[JsArray].value must not beEmpty
        }
        case _ => failure
      }
      success
    }

    "return multiple values" in {

      val params = "gradeLevels,reviewsPassed"
      val request = FakeRequest(GET, "/")
      val result = FieldValuesApi.multiple(params, None, "")(request)
      val json : JsValue = Json.parse(contentAsString(result))
      ((json \ "gradeLevels").asOpt[List[JsObject]].getOrElse(List()).length > 0)  === true
      ((json \ "reviewsPassed").asOpt[List[JsObject]].getOrElse(List()).length > 0) === true
    }

    "return multiple values with queries" in {
      val options = """{"subject" : { "q" : {"category": "Art"}} }"""
      val result = FieldValuesApi.multiple("subject,reviewsPassed", Some(options), "")(FakeRequest(GET, "/"))
      val json = Json.parse(contentAsString(result))
      (((json \ "subject")).asOpt[List[JsObject]].getOrElse(List()).length > 0) === true
    }

    "multiple handles invalid json" in {
      val result = FieldValuesApi.multiple("reviewsPassed", Some("asdfadsf"), "")(FakeRequest(GET, "/"))
      val json = contentAsJson(result)
      (((json\ "reviewsPassed")).asOpt[List[JsObject]].getOrElse(List()).length > 0) === true
    }

  }
}
