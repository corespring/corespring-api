package org.corespring.models

import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ColorPaletteTest extends Specification {

  "lighten" should {

    "convert to lighter color" in {
      ColorPalette.lighten("#84A783", 0.45) must be equalTo("#C7D7C7")
    }

  }

  "Reads" should {

    val prior = new ColorPalette("#FFFFFF", "#000000")
    val correctColor = "#FF00FF"
    val correctColorLight = "#00FF00"

    implicit val Reads = new ColorPalette.Reads(prior)

    "empty JSON" should {

      val json = Json.obj()
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "retain prior values" in {
        result.correctColor must be equalTo(prior.correctColor)
        result.correctColorLight must be equalTo(prior.correctColorLight)
      }

    }

    "only correctColor specified" should {

      val json = Json.obj("correctColor" -> correctColor)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "use correctColor from JSON" in {
        result.correctColor must be equalTo(correctColor)
      }

      "use default correctLightColor" in {
        result.correctColorLight must be equalTo(ColorPalette.Defaults.correctColorLight(result.correctColor))
      }

    }

    "correctColor and correctColorLight specified" should {

      val json = Json.obj("correctColor" -> correctColor, "correctColorLight" -> correctColorLight)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "use correctColor from JSON" in {
        result.correctColor must be equalTo(correctColor)
      }

      "use correctColorLight from JSON" in {
        result.correctColorLight must be equalTo(correctColorLight)
      }

    }

  }

}