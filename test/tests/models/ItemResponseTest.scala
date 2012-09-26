package tests.models

import org.specs2.mutable.Specification
import models.ItemResponse
import play.api.libs.json.{JsString, JsArray, Json}

class ItemResponseTest extends Specification{

  "ItemResponse" should {

    "parse json correctly with an array of values" in {
      val response = ItemResponse( id="test", outcome = "outcome" , value = "a" + ItemResponse.Delimiter + "b")
      val json = Json.toJson(response)
      (json \ "value") must equalTo( JsArray(Seq(JsString("a"), JsString("b"))))
    }

    "parse json correctly with a value string" in {
      val response = ItemResponse( id="test", outcome = "outcome" , value = "a,b")
      val json = Json.toJson(response)
      (json \ "value") must equalTo( JsString("a,b"))
    }
  }

}
