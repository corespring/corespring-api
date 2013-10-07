package org.corespring.poc.integration.impl.transformers.qti.interactions
import org.specs2.mutable.Specification
import scala.xml.transform.RuleTransformer
import play.api.libs.json.{JsObject, Json}
import scala.collection.mutable

class ChoiceInteractionTransformerTest extends Specification {


  val qti =
    <assessmentItem>
      <responseDeclaration identifier="Q_01" cardinality="single" baseType="identifier">
        <correctResponse>
          <value>ChoiceA</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <choiceInteraction responseIdentifier="Q_01" shuffle="false" maxChoices="1">
          <prompt>ITEM PROMPT?</prompt>
          <simpleChoice identifier="ChoiceA">ChoiceA text (Correct Choice)
            <feedbackInline identifier="ChoiceA" defaultFeedback="true"/>
          </simpleChoice>
          <simpleChoice identifier="ChoiceD">ChoiceD text
            <feedbackInline identifier="ChoiceD" defaultFeedback="true"/>
          </simpleChoice>
        </choiceInteraction>
      </itemBody>
    </assessmentItem>


  "ChoiceInteractionTransformer" should {
    "transform" in {

      val responseDeclarations = qti \\ "responseDeclaration"

      val componentsJson : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()

      val out = new RuleTransformer(new ChoiceInteractionTransformer.Rewriter(componentsJson, responseDeclarations )).transform(qti)

      componentsJson.get("Q_01") must beSome[JsObject]
    }
  }
}
