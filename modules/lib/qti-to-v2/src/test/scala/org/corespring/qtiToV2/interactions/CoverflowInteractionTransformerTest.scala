package org.corespring.qtiToV2.interactions

import org.corespring.qtiToV2.transformers.InteractionRuleTransformer
import org.specs2.mutable.Specification

import scala.xml.Node

class CoverflowInteractionTransformerTest extends Specification {

  "CoverflowInteractionTransformer" should {

    val component: Node = <corespring-multiple-choice/>

    val qti =
      <assessmentItem>
        <itemBody>
          <coverflow>{ component }</coverflow>
        </itemBody>
      </assessmentItem>

    def output = new InteractionRuleTransformer(CoverflowInteractionTransformer).transform(qti)
    def corespringCoverflows = (output \\ "corespring-coverflow")

    "should remove <coverflow/>" in {
      (output \\ "coverflow") must beEmpty
    }

    "should add <corespring-coverflow />" in {
      corespringCoverflows.length must be equalTo 1
    }

    "should add <corespring-coverflow /> with component as child" in {
      corespringCoverflows.head.child.contains(component) must beTrue
    }
  }

}
