package player.controllers.qti.rewrites

import scala.xml.transform.RewriteRule
import scala.xml._

object TexRewriteRule extends RewriteRule {

  val defaultInlineValue = true

  override def transform(node: Node) = node match {
    case node: Node if (node.label == "tex") => (node \ "@inline").text match {
      case "false" => blockTex(node)
      case _ => defaultTexTransform(node)
    }
    case _ => node
  }

  def defaultTexTransform(node: Node): Node = defaultInlineValue match {
    case true => inlineTex(node)
    case _ => blockTex(node)
  }

  def blockTex(node: Node) = Text(s"$$${node.child.mkString}$$")
  def inlineTex(node: Node) = Text(s"\\(${node.child.mkString}\\)")

}
