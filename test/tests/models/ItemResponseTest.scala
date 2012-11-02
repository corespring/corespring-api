package tests.models

import org.specs2.mutable.Specification
import models.{ArrayItemResponse, ItemResponseOutcome, ItemResponse}
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some

class ItemResponseTest extends Specification {

  "ItemResponse" should {

    val outcome = ItemResponseOutcome(score = 0,  comment = Some("b"))

    val response : ItemResponse = ArrayItemResponse(id = "test", outcome = Some(outcome), responseValue = Seq("a" ,"b"))
    val json = Json.toJson(response)

    val expectedJson = JsObject(Seq(
      "id" -> JsString("test"),
      "value" -> JsArray(Seq(JsString("a"), JsString("b"))),
      "outcome" -> JsObject(Seq(
        "score" -> JsNumber(0.0),
        "comment" -> JsString("b")
      ))
    ))

    "generate json correctly with an array of values" in {
      val expected = stringify(expectedJson)
      val actual = stringify(json)
      actual must equalTo(expected)
    }

    "not parse the outcome from the json" in {
      val parsedResponse: ItemResponse = expectedJson.as[ItemResponse]
      parsedResponse.outcome must beNone
    }

    "generate json correctly with a value string" in {
      val response : ItemResponse = ArrayItemResponse(id = "test", outcome = Some(outcome), responseValue = Seq("a","b"))
      val json = Json.toJson(response)
      println("json value: " + (json\"value"))
      (json \ "value") must equalTo(JsArray(Seq(JsString("a"),JsString("b"))))
    }
  }

}
