package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json._

import scala.xml._

object SelectPointInteractionTransformer extends InteractionTransformer {

  override def transform(node: Node) = {
    val identifier = (node \ "@responseIdentifier").text
    node match {
      case elem: Elem if (node.label == "selectPointInteraction") =>
        elem.child.filter(_.label != "object").map(n => n.label match {
          case "prompt" => <p class="prompt">{n.child}</p>
          case _ => n
        }) ++ <corespring-point-intercept id={identifier}></corespring-point-intercept>
      case _ => node
    }
  }

  override def interactionJs(qti: Node): Map[String, JsObject] = (qti \\ "selectPointInteraction").map(implicit node => {
    (node \ "@responseIdentifier").text -> Json.obj(
      "componentType" -> "corespring-point-intercept",
      "correctResponse" -> answers(qti)(node),
      "model" -> Json.obj(
        "config" -> partialObj(
          "domainLabel" -> property("xAxisTitle").map(JsString(_)),
          "rangeLabel" -> property("yAxisTitle").map(JsString(_)),
          "graphWidth" -> property("gridWidthInPixels").map(JsString(_)),
          "graphHeight" -> property("gridHeightInPixels").map(JsString(_)),
          "domain" -> property("xAxisMaxValue").map(f => JsNumber(f.toInt)),
          "range" -> property("yAxisMaxValue").map(f => JsNumber(f.toInt)),
          "maxPoints" -> Some(JsNumber((node \ "@maxChoices").text.toInt))
        )
      )
    )
  }).toMap

  private def answers(qti: Node)(implicit node: Node) = {
    (qti \\ "responseDeclaration").find(rd => (rd \ "@identifier").text == (node \ "@responseIdentifier").text)
      .map(rd => (rd \ "correctResponse" \ "value").toSeq.map(v =>  s"${(v \ "@xcoordinate").text},${(v \ "@ycoordinate").text}"))
  }

  private def property(name: String)(implicit node: Node): Option[String] =
    (node \ "object" \ "param").toSeq.find(p => (p \ "@name").text == name).map(p => (p \ "@value").text)
}
