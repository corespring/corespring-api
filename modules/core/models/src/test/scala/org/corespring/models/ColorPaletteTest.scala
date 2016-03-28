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

    import ColorPalette._

    val prior =
      ColorPalette("#FFFFFF", "#000000", "#FF00FF", "#00FF00", "#F0F0F0", "#FF0000", "#00FFFF", "#FFFF00", "#0000FF")
    val correctDark = "#AAAAAA"
    val correctLight = "#BBBBBB"
    val incorrectDark = "#CCCCCC"
    val incorrectLight = "#DDDDDD"
    val nothingSubmittedDark = "#EEEEEE"
    val nothingSubmittedLight = "#111111"
    val nothingSubmittedAccent = "#222222"
    val partiallyCorrectDark = "#333333"
    val partiallyCorrectLight = "#444444"

    implicit val Reads = new ColorPalette.Reads(prior)

    "empty JSON" should {

      val json = Json.obj()
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "retain prior values" in {
        result.correctDark must be equalTo (prior.correctDark)
        result.correctLight must be equalTo (prior.correctLight)
        result.incorrectDark must be equalTo (prior.incorrectDark)
        result.incorrectLight must be equalTo (prior.incorrectLight)
        result.nothingSubmittedDark must be equalTo (prior.nothingSubmittedDark)
        result.nothingSubmittedLight must be equalTo (prior.nothingSubmittedLight)
        result.nothingSubmittedAccent must be equalTo (prior.nothingSubmittedAccent)
        result.partiallyCorrectDark must be equalTo (prior.partiallyCorrectDark)
        result.partiallyCorrectLight must be equalTo (prior.partiallyCorrectLight)
      }

    }

    "JSON containing correctDark" should {

      val json = Json.obj(Fields.correctDark -> correctDark)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update correctDark" in {
        result.correctDark must be equalTo (correctDark)
      }

    }

    "JSON containing correctLight" should {

      val json = Json.obj(Fields.correctLight -> correctLight)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update correctLight" in {
        result.correctLight must be equalTo (correctLight)
      }

    }

    "JSON containing incorrectDark" should {

      val json = Json.obj(Fields.incorrectDark -> incorrectDark)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update incorrectDark" in {
        result.incorrectDark must be equalTo (incorrectDark)
      }

    }

    "JSON containing incorrectLight" should {

      val json = Json.obj(Fields.incorrectLight -> incorrectLight)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update incorrectLight" in {
        result.incorrectLight must be equalTo (incorrectLight)
      }

    }

    "JSON containing nothingSubmittedDark" should {

      val json = Json.obj(Fields.nothingSubmittedDark -> nothingSubmittedDark)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update nothingSubmittedDark" in {
        result.nothingSubmittedDark must be equalTo (nothingSubmittedDark)
      }

    }

    "JSON containing nothingSubmittedLight" should {

      val json = Json.obj(Fields.nothingSubmittedLight -> nothingSubmittedLight)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update nothingSubmittedLight" in {
        result.nothingSubmittedLight must be equalTo (nothingSubmittedLight)
      }

    }

    "JSON containing nothingSubmittedAccent" should {

      val json = Json.obj(Fields.nothingSubmittedAccent -> nothingSubmittedAccent)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update nothingSubmittedAccent" in {
        result.nothingSubmittedAccent must be equalTo (nothingSubmittedAccent)
      }

    }

    "JSON containing partiallyCorrectDark" should {

      val json = Json.obj(Fields.partiallyCorrectDark -> partiallyCorrectDark)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update partiallyCorrectDark" in {
        result.partiallyCorrectDark must be equalTo (partiallyCorrectDark)
      }

    }

    "JSON containing partiallyCorrectLight" should {

      val json = Json.obj(Fields.partiallyCorrectLight -> partiallyCorrectLight)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update partiallyCorrectLight" in {
        result.partiallyCorrectLight must be equalTo (partiallyCorrectLight)
      }

    }

  }

}