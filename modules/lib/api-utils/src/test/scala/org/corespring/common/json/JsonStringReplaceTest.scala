package org.corespring.common.json

import org.specs2.mutable.Specification
import play.api.libs.json._

class JsonStringReplaceTest extends Specification {

  import JsonStringReplace._

  "replaceStringsInJson" should {

    def addX(s: String) = {
      s"X$s"
    }
    "replace string on top-level" in {
      val json = replaceStringsInJson(Json.obj("s" -> "hi"), addX)
      json === Json.obj("s" -> "Xhi")
    }
    "replace string in nested object" in {
      val json = replaceStringsInJson(Json.obj("o" -> Json.obj("s" -> "hi")), addX)
      json === Json.obj("o" -> Json.obj("s" -> "Xhi"))
    }
    "replace string in nested array" in {
      val json = replaceStringsInJson(Json.obj("o" -> Json.arr("hi")), addX)
      json === Json.obj("o" -> Json.arr("Xhi"))
    }
    "not change booleans" in {
      val json = replaceStringsInJson(Json.obj("o" -> true), addX)
      json === Json.obj("o" -> true)
    }
    "not change numbers" in {
      val json = replaceStringsInJson(Json.obj("o" -> 56), addX)
      json === Json.obj("o" -> 56)
    }
    "not change nulls" in {
      val json = replaceStringsInJson(Json.obj("o" -> JsNull), addX)
      json === Json.obj("o" -> JsNull)
    }
  }
}