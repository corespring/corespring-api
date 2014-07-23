package org.corespring.qti.models.interactions

import org.corespring.qti.models.{QtiItem}
import org.specs2.mutable.Specification

class FeedbackInlineTest extends Specification {

  val qti = QtiItem(
    <assessmentItem>

      <correctResponseFeedback>Correct</correctResponseFeedback>
      <incorrectResponseFeedback>Incorrect</incorrectResponseFeedback>

      <responseDeclaration identifier='RESPONSE' cardinality='multiple' baseType='identifier'>
        <correctResponse>
          <value>A</value>
          <value>B</value>
        </correctResponse>
      </responseDeclaration>

      <outcomeDeclaration identifier='SCORE' cardinality='single' baseType='float'/>

      <itemBody>
        <choiceInteraction responseIdentifier='RESPONSE' shuffle="true" maxChoices='0'>
          <simpleChoice identifier='A' fixed='true'>A<feedbackInline identifier='A' defaultFeedback='true'/></simpleChoice>
          <simpleChoice identifier='B' fixed='true'>B<feedbackInline identifier='B' defaultFeedback='true'/></simpleChoice>
          <simpleChoice identifier='C' fixed='true'>C<feedbackInline identifier='C' defaultFeedback='true'/></simpleChoice>
          <simpleChoice identifier='D' fixed='true'>D<feedbackInline identifier='D' defaultFeedback='true'/></simpleChoice>
        </choiceInteraction>
      </itemBody>
    </assessmentItem>)


  "feedback inline" should {

    "feedback for choice that is part of the correct answer should be correct" in {
      val fb = FeedbackInline("", "RESPONSE", "A", "")
      fb.defaultContent(qti) mustEqual "Correct"
    }

    "feedback for choice that is not part of the correct answer should be correct" in {
      val fb = FeedbackInline("", "RESPONSE", "C", "")
      fb.defaultContent(qti) mustEqual "Incorrect"
    }
  }
}
