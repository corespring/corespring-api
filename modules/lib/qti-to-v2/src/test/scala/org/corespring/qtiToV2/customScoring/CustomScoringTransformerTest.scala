package org.corespring.qtiToV2.customScoring

import org.specs2.mutable.Specification

class CustomScoringTransformerTest extends Specification {

  "CustomScoring" should {
    "wrap the js" in {
      val transformer = new CustomScoringTransformer()
      transformer.generate("//qti-js", Map()) === transformer.template("//qti-js", Map())
    }
  }
}
