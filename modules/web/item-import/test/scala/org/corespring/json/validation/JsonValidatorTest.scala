package org.corespring.json.validation

import org.specs2.mutable.Specification
import play.api.libs.json._

class JsonValidatorTest extends Specification {

  "validateItem" should {

    val itemSchema =
      """
        |{
        |  "properties" : {
        |    "components" : {
        |      "type" : "object"
        |    }
        |  },
        |  "required" : [
        |    "components"
        |  ]
        |}
      """.stripMargin
    val itemValidator = new ItemValidator(s => Some(itemSchema))

    "reject empty json object if the schema requires a property" in {
      itemValidator.validate(Json.obj()) must beAnInstanceOf[Left[Seq[String], JsValue]]
    }

    "accept item with components and xhtml" in {
      itemValidator.validate(Json.obj("components" -> Json.obj(), "xhtml" -> "test")) must beAnInstanceOf[Right[Seq[String], JsValue]]
    }

  }

}