package org.corespring.qtiToV2.kds.interactions

import scala.xml.{NodeSeq, Node}

/**
 * Translate KDS <div class="exp"/> nodes into <sup/> (which is what they always should have been).
 */
object ExponentTransformer extends MathNotationTransformer {

  override def transform(node: Node) = node.label match {
    case "div" if (node \ "@class").text.contains("exp") => <sup/>.copy(child = node.child)
    case _ => node
  }

}
