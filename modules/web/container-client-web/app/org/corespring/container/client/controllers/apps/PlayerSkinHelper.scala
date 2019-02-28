package org.corespring.container.client.controllers.apps

import javax.xml.bind.DatatypeConverter
import play.api.libs.json.{ JsObject, JsValue, Json }

trait PlayerSkinHelper {

  def calculateColorToken(queryParams: JsObject, defaults: JsValue) = {
    val colorsParam = (queryParams \ "colors").asOpt[String]
    val defaultColors = (defaults \ "colors").asOpt[JsObject].getOrElse(Json.obj())
    val colorsJson = colorsParam match {
      case Some(colorsString) => (Json.parse(DatatypeConverter.parseBase64Binary(colorsString)) \ "colors").asOpt[JsObject].getOrElse(Json.obj())
      case None => Json.obj()
    }
    val computedColors = Json.obj("colors" -> (defaultColors ++ colorsJson))
    DatatypeConverter.printBase64Binary(computedColors.toString.getBytes)
  }

  def calculateIconSet(queryParams: JsObject, defaults: JsValue) = {
    val iconsetParam = (queryParams \ "iconSet").asOpt[String]
    val defaultIconSet = (defaults \ "iconSet").asOpt[String]
    iconsetParam.orElse(defaultIconSet).getOrElse("check")
  }

  def calculateColors(queryParams: JsObject, defaults: JsValue): JsObject = Json.obj(
    "correct-background" -> "#4aaf46",
    "correct-foreground" -> "#f8ffe2",
    "partially-correct-background" -> "#c1e1ac",
    "incorrect-background" -> "#fcb733",
    "incorrect-foreground" -> "#fbf2e3",
    "hide-show-background" -> "#bce2ff",
    "hide-show-foreground" -> "#1a9cff",
    "warning-background" -> "#464146",
    "warning-foreground" -> "#ffffff",
    "warning-block-background" -> "#e0dee0",
    "warning-block-foreground" -> "#f8f6f6",
    "muted-foreground" -> "#F8F6F6",
    "muted-background" -> "#E0DEE0") ++ (defaults \ "colors").asOpt[JsObject].getOrElse(Json.obj())

}
