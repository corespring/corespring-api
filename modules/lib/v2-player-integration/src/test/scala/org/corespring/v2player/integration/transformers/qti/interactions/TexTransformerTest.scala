package org.corespring.v2player.integration.transformers.qti.interactions

import org.specs2.mutable.Specification
import scala.xml.transform.RuleTransformer
import scala.xml.Node

class TexTransformerTest extends Specification {

  "TexTransformer" should {

    def qti(texNode: Node) =
      <assessmentItem>
        <itemBody>{ texNode }</itemBody>
      </assessmentItem>

    val tex = "This is my latex!"
    val texNode = <tex class="best-class">{ tex }</tex>

    def output = new RuleTransformer(TexTransformer).transform(qti(texNode))
    def corespringTex = (output \\ "corespring-tex")

    "should remove <tex/>" in {
      (output \\ "tex") must beEmpty
    }

    "should add <corespring-tex/>" in {
      corespringTex.length must be equalTo 1
    }

    "should add <corespring-tex/> with children" in {
      corespringTex.head.child diff texNode.child must beEmpty
    }

    "should preserve <tex/> attributes in <corespring-tex/>" in {
      corespringTex.head.attributes must be equalTo texNode.attributes
    }

  }

}
