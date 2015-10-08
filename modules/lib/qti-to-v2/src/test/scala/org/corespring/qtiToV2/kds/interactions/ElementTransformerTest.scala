package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.transformers.ItemTransformer
import org.specs2.mutable.Specification

import scala.xml.Node
import scala.xml.transform.{ RewriteRule, RuleTransformer }

class ElementTransformerTest extends Specification {

  def transform(node: Node) = new RuleTransformer(new RewriteRule {
    override def transform(node: Node) = ElementTransformer.transform(node, ItemTransformer.EmptyManifest)
  }).transform(node).head

  "transform" should {

    "transform <span class='under'/> to <u/>" in {
      val content = "Hey, I should be underlined!"
      transform(<span class="under">{ content }</span>).head must be equalTo (<u>{ content }</u>)
    }

  }

}
