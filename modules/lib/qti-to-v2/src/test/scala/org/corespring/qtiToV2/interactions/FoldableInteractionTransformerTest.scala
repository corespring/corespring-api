package org.corespring.qtiToV2.interactions

import org.corespring.qtiToV2.transformers.InteractionRuleTransformer
import org.specs2.mutable.Specification

import scala.xml.Node

class FoldableInteractionTransformerTest extends Specification {

  "FoldableInteractionTransformer" should {

    val component: Node = <corespring-multiple-choice/>

    val qti =
      <assessmentItem>
        <itemBody>
          <foldable>{ component }</foldable>
        </itemBody>
      </assessmentItem>

    def output = new InteractionRuleTransformer(FoldableInteractionTransformer).transform(qti)
    def corespringFoldables = (output \\ "div").filter(n => (n \ "@corespring-foldable").text == "corespring-foldable")

    "should remove <foldable/>" in {
      (output \\ "foldable") must beEmpty
    }

    "should add <div corespring-foldable='corespring-foldable'/>" in {
      corespringFoldables.length must be equalTo 1
    }

    "should add <div corespring-foldable='corespring-foldable'/> with component as child" in {
      corespringFoldables.head.child.contains(component) must beTrue
    }

  }

}
