package org.corespring.qtiToV2.kds.interactions

import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}

/**
 * Transforms KDS square root tables into MathJax
 */
object SquareRootTransformer extends MathNotationTransformer {

  val displayInlineNoBorder = Attribute(None, "style", Text("display: inline; border: none;"), Null)
  val displayInline = Attribute(None, "style", Text("display: inline; border: none;"), Null)

  override def transform(node: Node) = new RuleTransformer(new RewriteRule {
    override def transform(node: Node) = node match {
      case node: Elem if node.label == "table" && (node \ "@class").text.contains("verdana2t") => {
        new RuleTransformer(new RewriteRule {
          override def transform(node: Node) = node match {
            case tr: Elem if node.label == "tr" => tr.copy(child = node.child.sliding(2).map{
              pair =>
                if ((pair.head \ "entity" \ "@value").text == "8730" && (pair.last \ "@class").text == "vinculum")
                  <td style="display: inline; border: none; padding: 0;">
                    <span mathjax="">{s"""\\(\\displaystyle\\sqrt{${pair.last.text}}\\)"""}</span>
                  </td>
                else
                  pair.head match {
                    case e: Elem => e % displayInlineNoBorder
                    case _ => pair.head
                  }
            }.toSeq.withoutEmpty) % displayInline
            case tbody: Elem if node.label == "tbody" => tbody % displayInline
            case _ => node
          }
        }).transform(node)
      }
      case _ => node
    }
  }).transform(node)

  private implicit class NodeHelper(nodes: Seq[Node]) {
    def withoutEmpty = nodes.filter(node => {
      !node.child.text.trim.isEmpty
    })
  }

}
