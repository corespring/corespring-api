package org.corespring.models

import org.specs2.mutable.Specification
import play.api.libs.json.{JsError, Json}

class DisplayConfigTest extends Specification {

  "DisplayConfig" should {

    "iconSet" should {

      "with invalid value" should {

        "throws an exception" in {
           { DisplayConfig(iconSet = "invalid", colors = ColorPalette.default) } must throwAn[Exception]
        }

      }

      "with valid value" should {

        "set value" in {
          DisplayConfig.IconSets.sets.map{ iconSet => {
            DisplayConfig(iconSet = iconSet, colors = ColorPalette.default)
              .iconSet must be equalTo(iconSet)
          }}.tail
        }

      }

    }

    "Reads" should {

      implicit val Reads = new DisplayConfig.Reads(DisplayConfig.default)

      "invalid iconSet" should {

        val json = Json.obj("iconSet" -> "invalid")

        "return JsError" in {
          Json.fromJson[DisplayConfig](json) must haveClass[JsError]
        }

      }

    }

  }

}
