package org.corespring.qtiToV2.kds

import scala.xml.{Null, Elem, Node}
import scala.xml.transform.{RewriteRule, RuleTransformer}

trait XHTMLCleaner {

  val labelMap = Map("partBlock" -> "div", "partBody" -> "div", "selectedResponseParts" -> "div")

  def convertNonXHTMLElements(node: Node): Option[Node] = node.isEmpty match {
    case false => new RuleTransformer(new RewriteRule {
      override def transform(node: Node) = (node, labelMap.get(node.label)) match {
        case (e: Elem, Some(newLabel)) => e.copy(label = newLabel, attributes = Null)
        case _ => node
      }
    }).transform(node).headOption
    case _ => Some(node)
  }

}
