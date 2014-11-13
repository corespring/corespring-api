package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json._

import scala.xml.Node

object MatchInteractionTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node): Map[String, JsObject] = (qti \ "matchInteraction").map(node => {
    (node \ "responseIdentifier").text -> Json.obj()
  }).toMap

}
