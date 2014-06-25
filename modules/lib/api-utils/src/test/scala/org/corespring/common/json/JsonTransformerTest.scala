package org.corespring.common.json

import org.specs2.mutable.Specification
import play.api.libs.json._

class JsonTransformerTest extends Specification {

  "JsonTransformer" should {

    "do a simple transformation" in {

      object TestTransform extends JsonTransformer(
        ("apple.name" -> "myAppleName"))

      val in = Json.obj("apple" -> Json.obj("name" -> "granny smith"))
      val out = Json.obj("myAppleName" -> "granny smith")

      TestTransform.transform(in) === out
      TestTransform.reverseTransform(TestTransform.transform(in)) === in
    }

    "transform with merge" in {

      object TestTransform extends JsonTransformer(
        "appleName" -> "apple.name", "appleColour" -> "apple.colour")

      val in = Json.obj("appleName" -> "granny smith", "appleColour" -> "green")
      val out = Json.obj("apple" -> Json.obj("name" -> "granny smith", "colour" -> "green"))

      TestTransform.transform(in) === out
      TestTransform.reverseTransform(TestTransform.transform(in)) === in
    }

  }
}