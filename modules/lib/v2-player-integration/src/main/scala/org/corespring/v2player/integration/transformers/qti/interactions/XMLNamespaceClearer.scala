package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.{TopScope, Elem, Node}

trait XMLNamespaceClearer {

  def clearNamespace(x: Node):Node = x match {
    case e:Elem => e.copy(scope=TopScope, child = e.child.map(clearNamespace))
    case o => o
  }

}
