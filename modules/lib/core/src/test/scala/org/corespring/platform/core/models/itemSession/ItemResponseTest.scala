package org.corespring.platform.core.models.itemSession

import org.specs2.mutable.Specification
import play.api.libs.json.Json._
import play.api.libs.json._
import org.corespring.qti.models.responses.{ Response, ResponseOutcome, ArrayResponse, StringResponse }

class ResponseTest extends Specification {

  "Response" should {

    val outcome = ResponseOutcome(score = 0, isCorrect = false, comment = Some("b"))

    val response: Response = ArrayResponse(id = "test",
      outcome = Some(outcome),
      responseValue = Seq("a", "b"))
    val json = Json.toJson(response)

    val expectedJson = JsObject(Seq(
      "id" -> JsString("test"),
      "value" -> JsArray(Seq(JsString("a"), JsString("b"))),
      "outcome" -> JsObject(Seq(
        "score" -> JsNumber(0.0),
        "isCorrect" -> JsBoolean(false),
        "comment" -> JsString("b")))))

    "generate json correctly with an array of values" in {
      val expected = stringify(expectedJson)
      val actual = stringify(json)
      actual must equalTo(expected)
    }

    "not parse the outcome from the json" in {
      val parsedResponse: Response = expectedJson.as[Response]
      parsedResponse.outcome must beNone
    }

    "generate json correctly with a value string" in {
      val response: Response = ArrayResponse(id = "test", outcome = Some(outcome), responseValue = Seq("a", "b"))
      val json = Json.toJson(response)
      println("json value: " + (json \ "value"))
      (json \ "value") must equalTo(JsArray(Seq(JsString("a"), JsString("b"))))
    }
  }

}
