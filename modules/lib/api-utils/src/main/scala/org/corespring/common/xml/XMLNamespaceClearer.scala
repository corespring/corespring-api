package org.corespring.common.xml

import scala.xml.{Elem, Node, TopScope}

trait XMLNamespaceClearer {

  def clearNamespace(node: Node): Node = node match {
    case elem: Elem => elem.copy(scope = TopScope, child = elem.child.map(clearNamespace))
    case _ => node
  }

  def clearNamespace(seq: Seq[Node]): Seq[Node] = seq.map(clearNamespace)

}
