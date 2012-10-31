package tests.models

import org.specs2.mutable.Specification
import models.{ItemResponseOutcome, ItemResponse}
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some

class ItemResponseTest extends Specification {

  "ItemResponse" should {

    val outcome = ItemResponseOutcome(score = 0, maxScore = 0, comment = "b")

    val response = ItemResponse(id = "test", outcome = Some(outcome), value = "a" + ItemResponse.Delimiter + "b")
    val json = Json.toJson(response)

    val expectedJson = JsObject(Seq(
      "id" -> JsString("test"),
      "value" -> JsArray(Seq(JsString("a"), JsString("b"))),
      "outcome" -> JsObject(Seq(
        "score" -> JsNumber(0.0),
        "maxScore" -> JsNumber(0.0),
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
      val response = ItemResponse(id = "test", outcome = Some(outcome), value = "a,b")
      val json = Json.toJson(response)
      (json \ "value") must equalTo(JsString("a,b"))
    }
  }

}
