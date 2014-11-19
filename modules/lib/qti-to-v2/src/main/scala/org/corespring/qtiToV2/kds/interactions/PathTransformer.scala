package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json.JsObject

import scala.xml.{Null, Attribute, Elem, Node}

/**
 * KDS image and video resources are prefixed by ./ instead of / in markup, so we need to rewrite these.
 */
object PathTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node): Map[String, JsObject] = Map.empty[String, JsObject]

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if (Seq("img", "src").contains(node.label) && (node \ "@src").text.toString != "") => rewriteSrc(elem)
    case _ => node
  }

  private def rewriteSrc(elem: Elem) =
    elem % Attribute(null, "src", """\.\/(.*)""".r.replaceAllIn((elem \ "@src").text.toString, "$1"), Null)

}
