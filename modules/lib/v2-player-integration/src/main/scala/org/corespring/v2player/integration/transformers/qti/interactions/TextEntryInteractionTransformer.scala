package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml._
import play.api.libs.json._

object TextEntryInteractionTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node) = (qti \\ "textEntryInteraction").map(implicit node => {
    val correctResponses = (responseDeclaration(node, qti) \ "correctResponse" \\ "value").map(_.text).toSet

    (node \ "@responseIdentifier").text -> Json.obj(
      "componentType" -> "corespring-text-entry",
      "correctResponse" -> (correctResponses.size match {
        case 0 => Json.arr()
        case 1 => correctResponses.head
        case _ => correctResponses
      })
    )
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "textEntryInteraction" => {
      <corespring-text-entry id={(node \ "@responseIdentifier").text}></corespring-text-entry>
    }
    case _ => node
  }

}
