package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.Node

trait InteractionTransformer extends XMLNamespaceClearer {

  /**
   * Given a node and QTI document, method looks at node's responseIdentifier attribute, and finds a
   * <responseDeclaration/> within the QTI document whose identifier attribute matches.
   */
  def responseDeclaration(node: Node, qti: Node): Node = {
    (node \ "@responseIdentifier") match {
      case empty: Seq[Node] if empty.isEmpty => throw new IllegalArgumentException("Such bad")
      case nonEmpty: Seq[Node] => {
        val identifier = nonEmpty.text
        (qti \\ "responseDeclaration").find(n => (n \ "@identifier").text == identifier) match {
          case Some(node) => node
          case _ => throw new IllegalArgumentException(s"QTI does not contain responseDeclaration for $identifier")
        }
      }
    }
  }

}
