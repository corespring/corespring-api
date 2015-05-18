package org.corespring.qtiToV2

import org.specs2.mutable.Specification

class QtiTransformerTest extends Specification {
  "QtiTransformer" should {
    "convert itemBody into div.item-body" in {
      val out = QtiTransformer.transform(<assessmentItem><itemBody>Hi<div>a<div>b</div></div></itemBody></assessmentItem>)
      (out \ "xhtml").as[String] === """<div class="item-body">Hi<div>a<div>b</div></div></div>"""
    }
  }
}
