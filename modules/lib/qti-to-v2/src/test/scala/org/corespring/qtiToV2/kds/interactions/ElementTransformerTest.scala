package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification

class ElementTransformerTest extends Specification with ElementTransformer {

  "transformElements" should {

    "transform <span class='under'/> to <u/>" in {
      val content = "Hey, I should be underlined!"
      transformElements(<span class="under">{content}</span>).head must be equalTo(<u>{content}</u>)
    }

  }

}
