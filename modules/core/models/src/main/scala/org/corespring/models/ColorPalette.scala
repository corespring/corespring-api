package org.corespring.models

import play.api.libs.json._

case class ColorPalette(correctDark: String, correctLight: String, incorrectDark: String, incorrectLight: String,
                         nothingSubmittedDark: String, nothingSubmittedLight: String, nothingSubmittedAccent: String,
                         partiallyCorrectDark: String, partiallyCorrectLight: String)

object ColorPalette {

  object Defaults {
    val correctDark = "#8fa783"
    val correctLight = "#c7e2c7"
    val incorrectDark = "#eea236"
    val incorrectLight = "#fbe7b7"
    val nothingSubmittedDark = "#464146"
    val nothingSubmittedLight = "#ffffff"
    val nothingSubmittedAccent = "#e0dee0"
    val partiallyCorrectDark = "#3a86ad"
    val partiallyCorrectLight = "#c8e3e8"
  }

  object Fields {
    val correctDark = "correctDark"
    val correctLight = "correctLight"
    val incorrectDark = "incorrectDark"
    val incorrectLight = "incorrectLight"
    val nothingSubmittedDark = "nothingSubmittedDark"
    val nothingSubmittedLight = "nothingSubmittedLight"
    val nothingSubmittedAccent = "nothingSubmittedAccent"
    val partiallyCorrectDark = "partiallyCorrectDark"
    val partiallyCorrectLight = "partiallyCorrectLight"
  }

  object Writes extends Writes[ColorPalette] {

    import Fields._

    override def writes(colorPalette: ColorPalette): JsValue = Json.obj(
      correctDark -> colorPalette.correctDark,
      correctLight -> colorPalette.correctLight,
      incorrectDark -> colorPalette.incorrectDark,
      incorrectLight -> colorPalette.incorrectLight,
      nothingSubmittedDark -> colorPalette.nothingSubmittedDark,
      nothingSubmittedLight -> colorPalette.nothingSubmittedLight,
      nothingSubmittedAccent -> colorPalette.nothingSubmittedAccent,
      partiallyCorrectDark -> colorPalette.partiallyCorrectDark,
      partiallyCorrectLight -> colorPalette.partiallyCorrectLight
    )

  }

  class Reads(prior: ColorPalette) extends play.api.libs.json.Reads[ColorPalette] {

    import Fields._

    override def reads(json: JsValue): JsResult[ColorPalette] = JsSuccess(ColorPalette(
      correctDark = (json \ correctDark).asOpt[String].getOrElse(prior.correctDark),
      correctLight = (json \ correctLight).asOpt[String].getOrElse(prior.correctLight),
      incorrectDark = (json \ incorrectDark).asOpt[String].getOrElse(prior.incorrectDark),
      incorrectLight = (json \ incorrectLight).asOpt[String].getOrElse(prior.incorrectLight),
      nothingSubmittedDark = (json \ nothingSubmittedDark).asOpt[String].getOrElse(prior.nothingSubmittedDark),
      nothingSubmittedLight = (json \ nothingSubmittedLight).asOpt[String].getOrElse(prior.nothingSubmittedLight),
      nothingSubmittedAccent = (json \ nothingSubmittedAccent).asOpt[String].getOrElse(prior.nothingSubmittedAccent),
      partiallyCorrectDark = (json \ partiallyCorrectDark).asOpt[String].getOrElse(prior.partiallyCorrectDark),
      partiallyCorrectLight = (json \ partiallyCorrectLight).asOpt[String].getOrElse(prior.partiallyCorrectLight)
    ))
  }

  val default = ColorPalette(
    correctDark = Defaults.correctDark, correctLight = Defaults.correctLight,
    incorrectDark = Defaults.incorrectDark, incorrectLight = Defaults.incorrectLight,
    nothingSubmittedDark = Defaults.nothingSubmittedDark, nothingSubmittedLight = Defaults.nothingSubmittedLight,
    nothingSubmittedAccent = Defaults.nothingSubmittedAccent, partiallyCorrectDark = Defaults.partiallyCorrectDark,
    partiallyCorrectLight = Defaults.partiallyCorrectLight)

  /**
   * Note that these are probably unnecessary; we can handle this on the client end.
   */
  def lighten(hex: String, alpha: Double): String = {
    val (r,g,b) = rgb(hex)
    toHex(((1 - alpha) * 255 + alpha * r).toInt,
      ((1 - alpha) * 255 + alpha * g).toInt,
      ((1 - alpha) * 255 + alpha * b).toInt)
  }

  private def rgb(hex: String): (Int, Int, Int) = {
    val hexregex = "^#?([0-9A-F]{2})([0-9A-F]{2})([0-9A-F]{2})$".r
    hex match {
      case hexregex(r,g,b) => (Integer.parseInt(r, 16), Integer.parseInt(g, 16), Integer.parseInt(b, 16))
      case _ => (0,0,0)
    }
  }

  private def toHex(r: Int, g: Int, b: Int): String = s"#${r.toHexString}${g.toHexString}${b.toHexString}".toUpperCase

}