package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.Node

trait NodeUtils {

  /**
   * Performs an in-order traversal of an XML document, returning the first Node matching the provided predicate.
   */
  def inOrder(node: Node, predicate: Node => Boolean): Option[Node] = predicate(node) match {
    case true => Some(node)
    /* TODO: This should be optimized */
    case _ => node.child.map(child => inOrder(child, predicate)).flatten.find(_.nonEmpty)
  }

  /**
   * Returns an Option[String] representing which of the two provided labels was found "first" in the XML document. If
   * neither was found in the document, returns None.
   */
  def whichFirst(xml: Node, one: String, two: String, more: String*) =
    inOrder(xml, n => (more :+ one :+ two).contains(n.label)) match {
      case Some(node) => Some(node.label)
      case None => None
    }

}
