package org.corespring.container.client.controllers.helpers

import org.specs2.mutable.Specification
import play.api.libs.json._

class HelpersTest extends Specification with JsonHelper {

  "JsonHelper" should {

    "partialObj" should {

      val attr = "test"

      "return JSON attribute for Some" in {
        val value = "value!"
        val json = partialObj(
          attr -> Some(JsString(value)))
        (json \ attr).as[String] === value
      }

      "return no JSON attribute for None" in {
        val json = partialObj(
          attr -> None)
        (json \ attr) must beAnInstanceOf[JsUndefined]
      }

    }

  }

}
