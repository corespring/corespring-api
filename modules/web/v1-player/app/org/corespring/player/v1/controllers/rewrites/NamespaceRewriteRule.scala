package org.corespring.player.v1.controllers.rewrites

import org.corespring.qti.models.QtiItem
import scala.xml.transform.RewriteRule
import scala.xml.{Elem, Node}

object NamespaceRewriteRule extends RewriteRule {

  val NamespaceRegex = """xmlns.*?=".*?"""".r

  /**
   * remove the namespaces - Note: this is necessary to support correct rendering in IE8
   */
  def removeNamespaces(xml: String): String = NamespaceRegex.replaceAllIn(xml, "")

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem => QtiItem.interactionModels.find(i => i.interactionMatch(e)) match {
      case Some(i) => i.preProcessXml(e)
      case None => e
    }
    case other => other
  }

}
