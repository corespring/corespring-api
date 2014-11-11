package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json._

import scala.xml.Node

object HottextInteractionTransformer extends InteractionTransformer {

  //hottextInteraction
  override def transform(node: Node) = node match {
    case node: Node if (node.label == "hottextInteraction") =>
        <corespring-select-text id={(node \\ "@responseIdentifier").text}/>
    case _ => node
  }

  override def interactionJs(qti: Node) = (qti \\ "hottextInteraction").map(node => {
    (node \\ "@responseIdentifier").text ->
      Json.obj("model" -> Json.obj("choices" -> (node \\ "hottext").map(v => partialObj(
        "data" -> Some(JsString(v.text)),
        "correct" -> ((qti \\ "responseDeclaration")
          .find(rd => (rd \ "@identifier").text == (node \\ "@responseIdentifier").text)
          .map(rd => (rd \ "correctResponse" \\ "value").map(_.text)).getOrElse(Seq.empty)
          .contains((v \ "@identifier").text.toString) match {
            case true => Some(JsBoolean(true))
            case _ => None
        })
      ))))
  }).toMap

}
