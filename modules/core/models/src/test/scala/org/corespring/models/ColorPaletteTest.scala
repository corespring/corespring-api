package org.corespring.models

import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ColorPaletteTest extends Specification {

  "Reads" should {

    import ColorPalette._

    val prior =
      ColorPalette("#FFFFFF", "#000000", "#FF00FF", "#00FF00", "#F0F0F0", "#FF0000", "#00FFFF", "#FFFF00", "#0000FF",
        "#111222", "#222111", "#333444", "#555222")
    val correctBackground = "#AAAAAA"
    val correctForeground = "#BBBBBB"
    val partiallyCorrectBackground = "#CCCCCC"
    val incorrectBackground = "#DDDDDD"
    val incorrectForeground = "#EEEEEE"
    val hideShowBackground = "#111111"
    val hideShowForeground = "#222222"
    val warningBackground = "#333333"
    val warningForeground = "#444444"
    val warningBlockBackground = "#555555"
    val warningBlockForeground ="#666666"
    val mutedBackground = "#777777"
    val mutedForeground = "#888888"

    implicit val Reads = new ColorPalette.Reads(prior)

    "empty JSON" should {

      val json = Json.obj()
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "retain prior values" in {
        result.correctBackground must be equalTo (prior.correctBackground)
        result.correctForeground must be equalTo (prior.correctForeground)
        result.partiallyCorrectBackground must be equalTo (prior.partiallyCorrectBackground)
        result.incorrectBackground must be equalTo (prior.incorrectBackground)
        result.incorrectForeground must be equalTo (prior.incorrectForeground)
        result.hideShowBackground must be equalTo (prior.hideShowBackground)
        result.hideShowForeground must be equalTo (prior.hideShowForeground)
        result.warningBackground must be equalTo (prior.warningBackground)
        result.warningForeground must be equalTo (prior.warningForeground)
        result.warningBlockBackground must be equalTo(prior.warningBlockBackground)
        result.warningBlockForeground must be equalTo(prior.warningBlockForeground)
        result.mutedBackground must be equalTo(prior.mutedBackground)
        result.mutedForeground must be equalTo(prior.mutedForeground)
      }

    }

    "JSON containing correctBackground" should {

      val json = Json.obj(Fields.correctBackground -> correctBackground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update correctBackground" in {
        result.correctBackground must be equalTo (correctBackground)
      }

    }

    "JSON containing correctForeground" should {

      val json = Json.obj(Fields.correctForeground -> correctForeground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update correctForeground" in {
        result.correctForeground must be equalTo (correctForeground)
      }

    }

    "JSON containing partiallyCorrectBackground" should {

      val json = Json.obj(Fields.partiallyCorrectBackground -> partiallyCorrectBackground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update partiallyCorrectBackground" in {
        result.partiallyCorrectBackground must be equalTo (partiallyCorrectBackground)
      }

    }

    "JSON containing incorrectBackground" should {

      val json = Json.obj(Fields.incorrectBackground -> incorrectBackground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update incorrectBackground" in {
        result.incorrectBackground must be equalTo (incorrectBackground)
      }

    }

    "JSON containing incorrectForeground" should {

      val json = Json.obj(Fields.incorrectForeground -> incorrectForeground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update incorrectForeground" in {
        result.incorrectForeground must be equalTo (incorrectForeground)
      }

    }

    "JSON containing hideShowBackground" should {

      val json = Json.obj(Fields.hideShowBackground -> hideShowBackground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update hideShowBackground" in {
        result.hideShowBackground must be equalTo (hideShowBackground)
      }

    }

    "JSON containing hideShowForeground" should {

      val json = Json.obj(Fields.hideShowForeground -> hideShowForeground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update hideShowForeground" in {
        result.hideShowForeground must be equalTo (hideShowForeground)
      }

    }

    "JSON containing warningBackground" should {

      val json = Json.obj(Fields.warningBackground -> warningBackground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update warningBackground" in {
        result.warningBackground must be equalTo (warningBackground)
      }

    }

    "JSON containing warningForeground" should {

      val json = Json.obj(Fields.warningForeground -> warningForeground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update warningForeground" in {
        result.warningForeground must be equalTo (warningForeground)
      }

    }

    "JSON containing warningBlockBackground" should {

      val json = Json.obj(Fields.warningBlockBackground -> warningBlockBackground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update warningBlockBackground" in {
        result.warningBlockBackground must be equalTo (warningBlockBackground)
      }

    }

    "JSON containing warningBlockForeground" should {

      val json = Json.obj(Fields.warningBlockForeground -> warningBlockForeground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update warningBlockForeground" in {
        result.warningBlockForeground must be equalTo (warningBlockForeground)
      }

    }

    "JSON containing mutedBackground" should {

      val json = Json.obj(Fields.mutedBackground -> mutedBackground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update mutedBackground" in {
        result.mutedBackground must be equalTo (mutedBackground)
      }

    }

    "JSON containing mutedForeground" should {

      val json = Json.obj(Fields.mutedForeground -> mutedForeground)
      val result = Json.fromJson[ColorPalette](json).getOrElse(throw new Exception("Deserialization problem"))

      "update mutedForeground" in {
        result.mutedForeground must be equalTo (mutedForeground)
      }

    }
  }

}