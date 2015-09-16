package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json._

import scala.xml._

object CalculatorWidgetTransformer extends InteractionTransformer {

  // Only one calculator per item, so this should be acceptable.
  private val id = "CALCULATOR"

  override def interactionJs(qti: Node, manifest: Node): Map[String, JsObject] =
    getCalculatorType(manifest).map(calculatorOfType).getOrElse(Map.empty)

  override def transform(node: Node, manifest: Node): Seq[Node] = node match {
    case elem: Elem if (elem.label == "itemBody" && getCalculatorType(manifest).nonEmpty) =>
      elem.copy(child = calculatorNode +: elem.child)
    case _ => node
  }

  def calculatorNode = <corespring-calculator id={ id }></corespring-calculator>

  def calculatorOfType(calculatorType: String) = Map(id -> Json.obj(
    "weight" -> 0,
    "clean" -> true,
    "title" -> "Calculator",
    "isTool" -> true,
    "componentType" -> "corespring-calculator",
    "model" -> Json.obj(
      "config" -> Json.obj(
        "type" -> calculatorType))))

  private def getCalculatorType(manifest: NodeSeq) = ((manifest \\ "mathTools" \ "calculator").text.toLowerCase match {
    case calculatorType if (Seq("basic", "scientific").contains(calculatorType)) => Some(calculatorType)
    case _ => None
  })

}
