package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json._

import scala.xml._

object RulerWidgetTransformer extends InteractionTransformer {

  private val id = "RULER"

  private object Defaults {
    val Units = "imperial"
    val Label = "in"
    val Length = 12
    val PixelsPerUnit = 40
    val ticks = 16
  }

  override def interactionJs(qti: Node, manifest: Node): Map[String, JsObject] =
    getRuler(manifest).map(ruler => Map(id -> ruler)).getOrElse(Map.empty)

  override def transform(node: Node, manifest: Node): Seq[Node] = node match {
    case elem: Elem if (elem.label == "itemBody" && getRuler(manifest).nonEmpty) =>
      elem.copy(child = rulerNode +: elem.child)
    case _ => node
  }

  private def rulerNode = <corespring-ruler id={ id }></corespring-ruler>

  private def getRuler(manifest: Node): Option[JsObject] = (manifest \\ "mathTools" \ "ruler").headOption.map(ruler =>
    Json.obj(
      "weight" -> 0,
      "title" -> "Ruler",
      "componentType" -> "corespring-ruler",
      "isTool" -> true,
      "model" -> Json.obj(
        "config" -> Json.obj(
          "units" -> Seq("imperial", "metric").find(_ == (ruler \ "@units").text.toLowerCase).getOrElse(Defaults.Units),
          "label" -> Seq("in", "ft", "yd", "mi", "mm", "cm", "m", "km")
            .find(_ == (ruler \ "@label").text.toLowerCase).getOrElse(Defaults.Label),
          "length" -> int((ruler \ "@length").text, Defaults.Length),
          "pixelsPerUnit" -> int((ruler \ "@pixelsPerUnit").text, Defaults.PixelsPerUnit),
          "ticks" -> int((ruler \ "@ticks").text, Defaults.ticks)))))

  private def int(string: String, default: Int) = try {
    string.toInt
  } catch {
    case e: Exception => default
  }

}
