package org.corespring.models

import play.api.libs.json._

case class ColorPalette(correctBackground: String = ColorPalette.Defaults.correctBackground,
                        correctForeground: String = ColorPalette.Defaults.correctForeground,
                        partiallyCorrectBackground: String = ColorPalette.Defaults.partiallyCorrectBackground,
                        partiallyCorrectForeground: String = ColorPalette.Defaults.partiallyCorrectForeground,
                        incorrectBackground: String = ColorPalette.Defaults.incorrectBackground,
                        incorrectForeground: String = ColorPalette.Defaults.incorrectForeground,
                        hideShowBackground: String = ColorPalette.Defaults.hideShowBackground,
                        hideShowForeground: String = ColorPalette.Defaults.hideShowForeground,
                        warningBackground: String = ColorPalette.Defaults.warningBackground,
                        warningForeground: String = ColorPalette.Defaults.warningForeground,
                        warningBlockBackground: String = ColorPalette.Defaults.warningBlockBackground,
                        warningBlockForeground: String = ColorPalette.Defaults.warningBlockForeground,
                        mutedBackground: String = ColorPalette.Defaults.mutedBackground,
                        mutedForeground: String = ColorPalette.Defaults.mutedForeground)

object ColorPalette {

  object Defaults {
    val correctBackground = "#4aaf46"
    val correctForeground = "#f8ffe2"
    val partiallyCorrectBackground = "#c1e1ac"
    val partiallyCorrectForeground = "#4aaf46"
    val incorrectBackground = "#fcb733"
    val incorrectForeground = "#fbf2e3"
    val hideShowBackground = "#bce2ff"
    val hideShowForeground = "#1a9cff"
    val warningBackground = "#464146"
    val warningForeground = "#ffffff"
    val warningBlockBackground = "#e0dee0"
    val warningBlockForeground = "#f8f6f6"
    val mutedBackground = "#e0dee0"
    val mutedForeground = "#f8f6f6"
  }

  object Fields {
    val correctBackground = "correct-background"
    val correctForeground = "correct-foreground"
    val partiallyCorrectBackground = "partially-correct-background"
    val partiallyCorrectForeground = "partially-correct-foreground"
    val incorrectBackground = "incorrect-background"
    val incorrectForeground = "incorrect-foreground"
    val hideShowBackground = "hide-show-background"
    val hideShowForeground = "hide-show-foreground"
    val warningBackground = "warning-background"
    val warningForeground = "warning-foreground"
    val warningBlockBackground = "warning-block-background"
    val warningBlockForeground = "warning-block-foreground"
    val mutedForeground = "muted-foreground"
    val mutedBackground = "muted-background"
  }

  object Writes extends Writes[ColorPalette] {

    import Fields._

    override def writes(colorPalette: ColorPalette): JsValue = Json.obj(
      correctBackground -> colorPalette.correctBackground,
      correctForeground -> colorPalette.correctForeground,
      partiallyCorrectBackground -> colorPalette.partiallyCorrectBackground,
      partiallyCorrectForeground -> colorPalette.partiallyCorrectForeground,
      incorrectBackground -> colorPalette.incorrectBackground,
      incorrectForeground -> colorPalette.incorrectForeground,
      hideShowBackground -> colorPalette.hideShowBackground,
      hideShowForeground -> colorPalette.hideShowForeground,
      warningBackground -> colorPalette.warningBackground,
      warningForeground -> colorPalette.warningForeground,
      warningBlockBackground -> colorPalette.warningBlockBackground,
      warningBlockForeground -> colorPalette.warningBlockForeground,
      mutedBackground -> colorPalette.mutedBackground,
      mutedForeground -> colorPalette.mutedForeground
    )

  }

  class Reads(prior: ColorPalette) extends play.api.libs.json.Reads[ColorPalette] {

    import Fields._

    override def reads(json: JsValue): JsResult[ColorPalette] = JsSuccess(ColorPalette(
      correctBackground = (json \ correctBackground).asOpt[String].getOrElse(prior.correctBackground),
      correctForeground = (json \ correctForeground).asOpt[String].getOrElse(prior.correctForeground),
      partiallyCorrectBackground = (json \ partiallyCorrectBackground).asOpt[String].getOrElse(prior.partiallyCorrectBackground),
      partiallyCorrectForeground = (json \ partiallyCorrectForeground).asOpt[String].getOrElse(prior.partiallyCorrectForeground),
      incorrectBackground = (json \ incorrectBackground).asOpt[String].getOrElse(prior.incorrectBackground),
      incorrectForeground = (json \ incorrectForeground).asOpt[String].getOrElse(prior.incorrectForeground),
      hideShowBackground = (json \ hideShowBackground).asOpt[String].getOrElse(prior.hideShowBackground),
      hideShowForeground = (json \ hideShowForeground).asOpt[String].getOrElse(prior.hideShowForeground),
      warningBackground = (json \ warningBackground).asOpt[String].getOrElse(prior.warningBackground),
      warningForeground = (json \ warningForeground).asOpt[String].getOrElse(prior.warningForeground),
      warningBlockBackground = (json \ warningBlockBackground).asOpt[String].getOrElse(prior.warningBlockBackground),
      warningBlockForeground = (json \ warningBlockForeground).asOpt[String].getOrElse(prior.warningBlockForeground),
      mutedBackground = (json \ mutedBackground).asOpt[String].getOrElse(prior.mutedBackground),
      mutedForeground = (json \ mutedForeground).asOpt[String].getOrElse(prior.mutedForeground)
    ))
  }

  val default = ColorPalette(
    correctBackground = Defaults.correctBackground, correctForeground = Defaults.correctForeground,
    partiallyCorrectBackground = Defaults.partiallyCorrectBackground,
    partiallyCorrectForeground = Defaults.partiallyCorrectForeground,
    incorrectBackground = Defaults.incorrectBackground,
    incorrectForeground = Defaults.incorrectForeground, hideShowBackground = Defaults.hideShowBackground,
    hideShowForeground = Defaults.hideShowForeground, warningBackground = Defaults.warningBackground,
    warningForeground = Defaults.warningForeground, warningBlockBackground = Defaults.warningBlockBackground,
    warningBlockForeground = Defaults.warningBlockForeground,
    mutedBackground = Defaults.mutedBackground, mutedForeground = Defaults.mutedForeground)

}