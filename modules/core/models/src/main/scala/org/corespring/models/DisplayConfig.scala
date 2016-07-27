package org.corespring.models

import play.api.libs.json._

case class DisplayConfig(iconSet: String, colors: ColorPalette) {
  if (!DisplayConfig.IconSets.valid(iconSet)) throw new IllegalStateException(s"Invalid iconSet value '$iconSet'")
}

object DisplayConfig {

  object Fields {
    val iconSet = "iconSet"
    val colors = "colors"
  }

  object Defaults {
    val iconSet = "check"
    val colors = ColorPalette.default
  }

  object IconSets {
    val sets = Seq("emoji", "check")
    def valid(iconSet: String) = sets.contains(iconSet)
  }

  val default = DisplayConfig(iconSet = Defaults.iconSet, colors = Defaults.colors)

  class Reads(prior: DisplayConfig) extends play.api.libs.json.Reads[DisplayConfig] {
    implicit val ColorPaletteReads = new ColorPalette.Reads(prior.colors)

    override def reads(json: JsValue): JsResult[DisplayConfig] = {
      val iconSet = (json \ Fields.iconSet).asOpt[String].getOrElse(Defaults.iconSet)
      DisplayConfig.IconSets.valid(iconSet) match {
        case true => Json.fromJson[ColorPalette](json \ Fields.colors) match {
          case JsSuccess(colors, _) =>
            JsSuccess(DisplayConfig(
              iconSet = (json \ Fields.iconSet).asOpt[String].getOrElse(Defaults.iconSet), colors = colors))
          case error: JsError => error
        }
        case _ => JsError(s"Invalid iconSet value '$iconSet'")
      }
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
