package org.corespring.models

import play.api.libs.json._

case class ColorPalette(correctColor: String, correctColorLight: String)

object ColorPalette {

  object Defaults {
    val correctColor = "#84A783"
    def correctColorLight(correctColor: String) = lighten(correctColor, 0.45)
  }

  object Fields {
    val correctColor = "correctColor"
    val correctColorLight = "correctColorLight"
  }

  object Writes extends Writes[ColorPalette] {

    import Fields._

    override def writes(colorPalette: ColorPalette): JsValue = Json.obj(
      correctColor -> colorPalette.correctColor,
      correctColorLight -> colorPalette.correctColorLight
    )

  }

  class Reads(prior: ColorPalette) extends play.api.libs.json.Reads[ColorPalette] {

    import Fields._

    override def reads(json: JsValue): JsResult[ColorPalette] = (json \ correctColor).asOpt[String] match {
      case Some(correctColor) => (json \ correctColorLight).asOpt[String] match {
        case Some(correctColorLight) => JsSuccess(ColorPalette(correctColor, correctColorLight))
        case _ => JsSuccess(ColorPalette(correctColor, Defaults.correctColorLight(correctColor)))
      }
      case None => JsSuccess(prior)
    }
  }

  val default = ColorPalette(correctColor = Defaults.correctColor)

  def apply(correctColor: String = Defaults.correctColor, correctColorLight: Option[String] = None): ColorPalette =
    ColorPalette(correctColor, correctColorLight.getOrElse(Defaults.correctColorLight(Defaults.correctColor)))

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