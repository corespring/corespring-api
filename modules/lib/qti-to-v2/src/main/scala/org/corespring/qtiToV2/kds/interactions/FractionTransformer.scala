package org.corespring.qtiToV2.kds.interactions

import scala.xml.Node
import scala.xml.transform._

trait FractionTransformer {

  def transformFractions(node: Node) = new RuleTransformer(new RewriteRule {
    override def transform(node: Node) = node match {
      case node: Node if (node.label == "table" && (node \ "@class").text == "frac" && !containsInteraction(node)) =>
        (findTextByClass(node, "nu"), findTextByClass(node, "de")) match {
          case (Some(numerator), Some(denominator)) if (numerator.trim.nonEmpty && denominator.trim.nonEmpty) =>
            <span mathjax="">{s"""\\(\\displaystyle\\frac{$numerator}{$denominator}\\)"""}</span>
          case _ => node
        }
      case _ => node
    }
  }).transform(node)

  private def findTextByClass(node: Node, clazz: String): Option[String] = {
    (node \ "@class").text.contains(clazz) match {
      case true => Some(node.text.trim)
      case _ => node.child.find(child => findTextByClass(child, clazz).nonEmpty) match {
        case Some(node) => Some(node.text.trim)
        case _ => None
      }
    }
  }

  private def containsInteraction(table: Node) = (table \\ "textEntryInteraction").nonEmpty

}