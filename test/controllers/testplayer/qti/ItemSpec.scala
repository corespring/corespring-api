package controllers.testplayer.qti

import org.specs2.mutable._

class ItemSpec extends Specification {

  class FeedbackSequenceMatcher(feedback: Seq[FeedbackElement]) {
    def matches(ids: List[Int]): Boolean = feedback.map(_.csFeedbackId.toInt).toSet equals ids.toSet
  }

  implicit def feedbackSeqToMatcher(feedback: Seq[FeedbackElement]) = new FeedbackSequenceMatcher(feedback)

  "A mutiple choice item" should {


    val correctFeedbackIds = List[Int](1,2)
    val incorrectFeedbackIds = List[Int](3,4,5,6)

    val xml = <assessmentItem title="" adaptive="false" timeDependent="false" xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <responseDeclaration identifier="RESPONSE" cardinality="single" baseType="identifier">
        <correctResponse>
          <value>ChoiceB</value>
        </correctResponse>
      </responseDeclaration>
      <outcomeDeclaration identifier="SCORE" cardinality="single" baseType="integer">
        <defaultValue>
          <value>0</value>
        </defaultValue>
      </outcomeDeclaration>
      <itemBody>
        <choiceInteraction responseIdentifier="RESPONSE" shuffle="false" maxChoices="1">
          <prompt>Which WWII code-breaker is widely considered to be the father of computer science and artificial intelligence?</prompt>
          <simpleChoice identifier="ChoiceA">
            Ada Lovelace
            <feedbackInline showHide="show" outcomeIdentifier="SCORE" identifier="0" csFeedbackId={incorrectFeedbackIds(0).toString}>Sorry. That's not the right answer.</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="ChoiceB">
            Alan Turing
            <feedbackInline showHide="show" outcomeIdentifier="SCORE" identifier="1" csFeedbackId={correctFeedbackIds(0).toString}>That's right!</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="ChoiceC">Edsger Dijkstra
            <feedbackInline showHide="show" outcomeIdentifier="SCORE" identifier="0" csFeedbackId={incorrectFeedbackIds(1).toString}>I'm sorry, but that's not right.</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="ChoiceD">Donald Knuth
            <feedbackInline showHide="show" outcomeIdentifier="SCORE" identifier="0" csFeedbackId={incorrectFeedbackIds(2).toString}>Wrong! Try again.</feedbackInline>
          </simpleChoice>

        </choiceInteraction>
        <modalFeedback identifier="1" outcomeIdentifier="SCORE" csFeedbackId={correctFeedbackIds(1).toString}>Correct!</modalFeedback>
        <modalFeedback identifier="0" outcomeIdentifier="SCORE" csFeedbackId={incorrectFeedbackIds(3).toString}>That's wrong. As a hint, here's a picture: <img src="http://upload.wikimedia.org/wikipedia/en/c/c8/Alan_Turing_photo.jpg" /></modalFeedback>
      </itemBody>
    </assessmentItem>

    val item = new QtiItem(xml)

    "should return correct feedback for correct choice" in {
      if (item.feedback("RESPONSE", "ChoiceB") matches correctFeedbackIds) success else failure
    }

    // TODO: This is probably wrong.. each incorrect answer probably wants to show its own feedback.
    "should return incorrect feedback for incorrect choice" in {
      if (item.feedback("RESPONSE", "ChoiceA") matches incorrectFeedbackIds) success else failure
    }
  }

}
