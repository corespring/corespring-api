package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json.JsObject

import scala.xml.{Null, Attribute, Elem, Node}

object ImagePathTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node): Map[String, JsObject] = Map.empty[String, JsObject]

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if (node.label == "img" && (node \ "@src").text.toString != "") =>
      elem % Attribute(null, "src", """\.\/(.*)""".r.replaceAllIn((node \ "@src").text.toString, "$1"), Null)
    case _ => node
  }

}
