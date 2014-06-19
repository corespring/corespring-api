package org.corespring.qtiToV2.interactions

import scala.xml._

abstract class NodeReplacementTransformer extends InteractionTransformer {

  def labelToReplace: String
  def replacementNode: Elem

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == labelToReplace => replacementNode.copy(child = elem.child.map(clearNamespace))
    case _ => node
  }

  override def interactionJs(qti: Node) = Map.empty[String, JsObject]

}
