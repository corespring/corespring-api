package org.corespring.qtiToV2.kds.interactions

import org.corespring.platform.core.utils.NumberParsers
import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json._

import scala.xml.Node

object NumberLineInteractionTransformer extends InteractionTransformer with NumberParsers {

  object Defaults {
    val SnapPerTick = 1
  }

  override def transform(node: Node, manifest: Node) = node match {
    case node: Node if (node.label == "numberLineInteraction") =>
      <p class="prompt">{ (node \ "prompt").map(_.child).flatten }</p> ++
        <corespring-number-line id={ (node \\ "@responseIdentifier").text }/>
    case _ => node
  }

  override def interactionJs(qti: Node, manifest: Node): Map[String, JsObject] =
    (qti \\ "numberLineInteraction").map(implicit node => {
      (node \ "@responseIdentifier").text -> Json.obj(
        "componentType" -> "corespring-number-line",
        "correctResponse" -> correctResponses(qti).map(value => Json.obj(
          "type" -> "point",
          "pointType" -> "full",
          "domainPosition" -> value.toDouble)),
        "model" -> Json.obj(
          "config" -> Json.obj(
            "domain" -> Seq((node \ "@lowerBound").text.toDouble, (node \ "@upperBound").text.toDouble),
            "initialType" -> "PF",
            "snapPerTick" -> 1,
            "showMinorTicks" -> JsBoolean((node \ "@displayMinorTickMarks").text == "true"),
            "exhibitOnly" -> false,
            "maxNumberOfPoints" -> correctResponses(qti).length,
            "snapPerTick" -> parseInt((node \ "@minorTickMarkFreq").text).getOrElse(Defaults.SnapPerTick).asInstanceOf[Int],
            "tickFrequency" -> (((node \ "@upperBound").text.toDouble - (node \ "@lowerBound").text.toDouble) / (node \ "@step").text.toDouble),
            "availableTypes" -> Json.obj("PF" -> true),
            "initialElements" -> Json.arr(),
            "ticks" -> (node \ "hatchMark")
              .map(hatch => Json.obj(
                "label" -> (if ((hatch \ "@isVisibleLabel").text == "1") (hatch \ "@label").text else ""),
                "value" -> BigDecimal((hatch \ "@value").text))))))
    }).toMap

  def correctResponses(qti: Node)(implicit node: Node) = (qti \\ "responseDeclaration")
    .find(rd => (rd \ "@identifier").text == (node \ "@responseIdentifier").text)
    .map(rd => (rd \ "correctResponse" \ "value").toSeq.map(_.text))
    .getOrElse(throw new Exception("Could not find response declaration"))

}
