package org.corespring.player.v1.controllers.rewrites

import org.apache.commons.lang3.StringEscapeUtils
import scala.xml._
import scala.xml.transform.RewriteRule

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

  def blockTex(node: Node) = Text(s"$$$$${nodeText(node)}$$$$")
  def inlineTex(node: Node) = Text(s"\\(${nodeText(node)}\\)")

  private def nodeText(node: Node) = StringEscapeUtils.unescapeHtml4(node.child.mkString)

}
