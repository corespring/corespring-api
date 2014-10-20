package org.corespring.json.validation

import org.specs2.mutable.Specification
import play.api.libs.json._

class JsonValidatorTest extends Specification {

  "validateItem" should {

    "reject empty json object" in {
      JsonValidator.validateItem(Json.obj()) must beAnInstanceOf[Left[Seq[String], JsValue]]
    }

    "accept item with components and xhtml" in {
      JsonValidator.validateItem(Json.obj("components" -> Json.obj(), "xhtml" -> "test")) must beAnInstanceOf[Right[Seq[String], JsValue]]
    }

  }

}