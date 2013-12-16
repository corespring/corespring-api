package org.corespring.qti.models.interactions

import org.specs2.mutable.Specification
import org.corespring.qti.models.{ResponseDeclaration, QtiItem}
import org.corespring.qti.models.responses.StringResponse

class TextEntryInteractionTest extends Specification{
  val qti = QtiItem(
    <assessmentItem>
      <responseDeclaration identifier="equationEntryTest" cardinality="single" baseType="line" exactMatch="false">
        <correctResponse>
          <value>y=2x+7</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <textEntryInteraction responseIdentifier="equationEntryTest" expectedLength="20" />

        <feedbackBlock outcomeIdentifier="responses.equationEntryTest.outcome.incorrectEquation">
          <div class="feedback-block-incorrect">Good try.</div>
        </feedbackBlock>

        <feedbackBlock outcomeIdentifier="responses.equationEntryTest.value" identifier="y=2x+7">
          <div class="feedback-block-correct">Correct!</div>
        </feedbackBlock>

        <feedbackBlock outcomeIdentifier="responses.equationEntryTest.outcome.lineEquationMatch">
          <div class="feedback-block-correct">You're right, but equation is not in form of y=mx+b</div>
        </feedbackBlock>
      </itemBody>
    </assessmentItem>)

  val optte1:Option[TextEntryInteraction] = qti.itemBody.interactions.find(i => i.responseIdentifier == "equationEntryTest" && i.isInstanceOf[TextEntryInteraction]).map(_.asInstanceOf[TextEntryInteraction])
  val optrd1 = qti.responseDeclarations.find(_.identifier == "equationEntryTest")

  "qti item 1" should {
    "contain text entry interaction" in {
      optte1 must beSome[TextEntryInteraction]
    }
    "contain response declaration" in {
      optrd1 must beSome[ResponseDeclaration]
    }
    "retrieves correct response for exact match" in {
      val optro = optte1.get.getOutcome(optrd1, StringResponse("equationEntryTest","y=2x+7",None))
      optro.map(ro => ro.isCorrect) must beSome(true)
      optro.map(ro =>ro.outcomeProperties.isEmpty) must beSome(true)
    }
    "retrieves correct response with spaces" in {
      val optro = optte1.get.getOutcome(optrd1, StringResponse("equationEntryTest","y = 2x + 7",None))
      optro.map(ro => ro.isCorrect) must beSome(true)
      optro.map(ro =>ro.outcomeProperties.isEmpty) must beSome(true)
    }
    "retrieves correct response with outcome properties when necessary" in {
      val optro = optte1.get.getOutcome(optrd1, StringResponse("equationEntryTest","y - 2x = 7",None))
      optro.map(ro => ro.isCorrect) must beSome(true)
      optro.map(ro =>ro.outcomeProperties.exists(prop => prop._1 == "lineEquationMatch" && prop._2)) must beSome(true)
    }
  }
}
