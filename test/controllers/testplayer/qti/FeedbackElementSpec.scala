package controllers.testplayer.qti

import org.specs2.mutable._

class FeedbackElementSpec extends Specification {

  "A feedback element" should {

    val feedbackXML =
      <feedbackInline identifier="1" outcomeIdentifier="SCORE">
        <p>This is the correct answer!</p>
        <p>You're awesome at this!</p>
      </feedbackInline>

    val feedbackElement = new FeedbackElement(feedbackXML)

    "parse identifier and outcome identifier" in {
      if (feedbackElement.identifier equals "1") success else failure
      if (feedbackElement.outcomeIdentifier equals "SCORE") success else failure
    }

    "match on matching identifier and outcome identifier" in {
       if (feedbackElement.matches("SCORE", "1")) success else failure
    }

    "not match on mismatched outcome identifier" in {
       if (feedbackElement.matches("VALUE", "1")) failure else success
    }

    "not match on mismatched identifier" in {
      if (feedbackElement.matches("SCORE", "300")) failure else success
    }

    "return body xml" in {
      if (feedbackElement.body equals feedbackXML.toString) success else failure
    }

  }


}