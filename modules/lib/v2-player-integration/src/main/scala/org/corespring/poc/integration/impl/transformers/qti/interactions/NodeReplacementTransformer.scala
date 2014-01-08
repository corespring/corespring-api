package org.corespring.poc.integration.impl.transformers.qti.interactions

import scala.xml.{Elem, Node}
import scala.xml.transform.RewriteRule

abstract class NodeReplacementTransformer() extends RewriteRule with XMLNamespaceClearer {

  def labelToReplace: String
  def replacementNode: Elem

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == labelToReplace => {
      replacementNode.copy(child = elem.child.map(clearNamespace))
    }
    case _ => node
  }

}
