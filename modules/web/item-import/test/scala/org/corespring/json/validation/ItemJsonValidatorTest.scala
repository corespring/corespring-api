package org.corespring.importing.validation

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json._

import scalaz.{ Failure, Success }

class ItemJsonValidatorTest extends Specification {

  val schemaString =
    """
      |{
      |  "title" : "CoreSpring Item Schema",
      |  "type" : "object",
      |  "properties" : {
      |    "files" : {
      |      "type" : "array",
      |      "items" : {
      |        "type" : "string"
      |      }
      |    },
      |    "components" : {
      |      "type" : "object"
      |    },
      |    "summaryFeedback" : {
      |      "type" : "string"
      |    },
      |    "xhtml" : {
      |      "type" : "string"
      |    }
      |  },
      |  "required" : [
      |    "components", "xhtml"
      |  ]
      |}
    """.stripMargin

  "validateItem" should {

    trait validate extends Scope {
      val schema = ItemSchema(schemaString)
      val validator = new ItemJsonValidator(schema)
    }

    "reject empty json object" in new validate {
      validator.validate(Json.obj()) must beAnInstanceOf[Failure[Seq[String], JsValue]]
    }

    "accept item with components and xhtml" in new validate {
      val json = Json.obj("components" -> Json.obj(), "xhtml" -> "test")
      validator.validate(json) must beAnInstanceOf[Success[Seq[String], JsValue]]
    }

  }

}