package org.corespring.qtiToV2.interactions

import play.api.libs.json.JsObject

import scala.xml.{Elem, Node}

/**
 * Removes style="font-size:2pt;" if it exists on a node.
 */
object FontTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node): Map[String, JsObject] = Map.empty

  override def transform(node: Node) = {
    node match {
      case elem: Elem if (elem \ "@style").text.contains("font-size:2pt") =>
        elem.copy(attributes = elem.attributes.remove("style"))
      case _ => node
    }
  }

}
