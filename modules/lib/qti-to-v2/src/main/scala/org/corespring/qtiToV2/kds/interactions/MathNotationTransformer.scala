package org.corespring.qtiToV2.kds.interactions

import scala.xml._
import scala.xml.transform._

/**
 * A trait specifically to translate KDS's weird math notations into usable HTML and/or MathJax
 */
trait MathNotationTransformer {
  def transform(node: Node): NodeSeq
}

object MathNotationTransformer {

  val transformers = Seq(
    FractionTransformer,
    SquareRootTransformer,
    ExponentTransformer)

  def transform(node: Node): Node = {
    transformers.foldLeft(node){ (acc, transformer) => {
      new RuleTransformer(new RewriteRule {
        override def transform(node: Node) = transformer.transform(node)
      }).transform(acc).head
    }}
  }

}
