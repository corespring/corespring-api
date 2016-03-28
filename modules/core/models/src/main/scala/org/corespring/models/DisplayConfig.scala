package org.corespring.models

import play.api.libs.json._

class DisplayConfig private (val iconSet: String, val colors: ColorPalette)

object DisplayConfig {

  object Fields {
    val iconSet = "iconSet"
    val colors = "colors"
  }

  object Defaults {
    val iconSet = "emoji"
    val colors = ColorPalette.default
  }

  object IconSets {
    val sets = Seq("emoji", "check")
    def valid(iconSet: String) = sets.contains(iconSet)
  }

  def apply(iconSet: String, colors: ColorPalette) = IconSets.valid(iconSet) match {
    case true => new DisplayConfig(iconSet, colors)
    case _ => new DisplayConfig(Defaults.iconSet, colors)
  }

  val default = DisplayConfig(iconSet = Defaults.iconSet, colors = Defaults.colors)

  class Reads(prior: DisplayConfig) extends play.api.libs.json.Reads[DisplayConfig] {
    import Fields._
    implicit val ColorPaletteReads = new ColorPalette.Reads(prior.colors)

    override def reads(json: JsValue): JsResult[DisplayConfig] = Json.fromJson[ColorPalette](json \ colors) match {
      case JsSuccess(colors, _) =>
        JsSuccess(DisplayConfig(iconSet = (json \ iconSet).asOpt[String].getOrElse(Defaults.iconSet), colors = colors))
      case error: JsError => error
    }

  }

  object Writes extends Writes[DisplayConfig] {
    import Fields._
    implicit val ColorPaletteWrites = ColorPalette.Writes

    override def writes(displayConfig: DisplayConfig): JsValue = Json.obj(
      iconSet -> displayConfig.iconSet,
      colors -> Json.toJson[ColorPalette](displayConfig.colors)
    )
  }

}
