package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.{TopScope, Elem, Node}

trait XMLNamespaceClearer {

  def clearNamespace(node: Node): Node = node match {
    case elem: Elem => elem.copy(scope = TopScope, child = elem.child.map(clearNamespace))
    case _ => node
  }

  def clearNamespace(seq: Seq[Node]): Seq[Node] = seq.map(clearNamespace)

}
