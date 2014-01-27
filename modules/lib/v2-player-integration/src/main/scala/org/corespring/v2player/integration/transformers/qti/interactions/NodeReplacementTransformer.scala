package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.{Elem, Node}
import scala.xml.transform.RewriteRule
import play.api.libs.json.JsObject

abstract class NodeReplacementTransformer extends InteractionTransformer {

  def labelToReplace: String
  def replacementNode: Elem

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == labelToReplace => {
      replacementNode.copy(child = elem.child.map(clearNamespace))
    }
    case _ => node
  }

  override def interactionJs(qti: Node) = Map.empty[String, JsObject]

}
