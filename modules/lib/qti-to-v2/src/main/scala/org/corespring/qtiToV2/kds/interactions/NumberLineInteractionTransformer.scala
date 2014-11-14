package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json.{Json, JsObject}

import scala.xml.Node

object NumberLineInteractionTransformer extends InteractionTransformer {

  override def transform(node: Node) = node match {
    case node: Node if (node.label == "numberLineInteraction") =>
      <p class="prompt">{(node \ "prompt").map(_.child).flatten}</p> ++
          <corespring-number-line id={(node \\ "@responseIdentifier").text}/>
    case _ => node
  }

  override def interactionJs(qti: Node): Map[String, JsObject] = {
    val rv = (qti \\ "numberLineInteraction").map(implicit node => {
      (node \ "@responseIdentifier").text -> Json.obj(
        "componentType" -> "corespring-number-line",
        "correctResponse" -> correctResponses(qti).map(value => Json.obj(
          "type" -> "point",
          "pointType" -> "full",
          "domainPosition" -> value.toDouble
        )),
        "model" -> Json.obj(
          "config" -> Json.obj(
            "domain" -> Seq((node \ "@lowerBound").text.toDouble, (node \ "@upperBound").text.toDouble),
            "initialType" -> "PF",
            "snapPerTick" -> 100,
            "showMinorTicks" -> true,
            "exhibitOnly" -> false,
            "maxNumberOfPoints" -> correctResponses(qti).length,
            "tickFrequency" -> (((node \ "@upperBound").text.toDouble - (node \ "@lowerBound").text.toDouble)/(node \ "@step").text.toDouble),
            "availableTypes" -> Json.obj("PF" -> true),
            "objects" -> Json.arr()
          )
        )
      )
    }).toMap
    rv.headOption match {
      case Some(thing) => println(Json.prettyPrint(thing._2))
      case _ => {}
    }
    rv
  }

  def correctResponses(qti: Node)(implicit node: Node) = (qti \\ "responseDeclaration")
    .find(rd => (rd \ "@identifier").text == (node \ "@responseIdentifier").text)
    .map(rd => (rd \ "correctResponse" \ "value").toSeq.map(_.text))
    .getOrElse(throw new Exception("Could not find response declaration"))

}
