package org.corespring.qti.models

import org.corespring.qti.models.interactions.FeedbackInline
import org.specs2.mutable._

class FeedbackElementTest extends Specification {

  "A feedback element" should {

    val feedbackXML =
      <feedbackInline identifier="1" outcomeIdentifier="responses.SCORE.value">
        <p>This is the correct answer!</p>
        <p>You're awesome at this!</p>
      </feedbackInline>

    val feedbackInline = FeedbackInline(feedbackXML, None)

    "parse identifier and outcome identifier" in {
      if (feedbackInline.identifier equals "1") success else failure
      if (feedbackInline.outcomeIdentifier equals "SCORE") success else failure
    }

    "return body xml" in {
      val actualString = feedbackXML.child.foldRight[String]("")((node, acc) => node.toString() + acc)
      feedbackInline.content must beEqualTo(actualString.trim)
    }

  }

}