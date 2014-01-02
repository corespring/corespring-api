package org.corespring.poc.integration.impl.transformers.qti.interactions

import org.specs2.mutable.Specification
import scala.xml.Node
import scala.collection.mutable
import play.api.libs.json.JsObject
import scala.xml.transform.RuleTransformer

class TextEntryInteractionTransformerTest extends Specification {

  val identifier = "Q_01"

  def qti(correctResponses: Seq[String], correctFeedback: String, incorrectFeedback: String): Node =
    <assessmentItem>
      <responseDeclaration identifier={identifier} cardinality="single" baseType="string">
        <correctResponse>
          {correctResponses.map(response => <value>{response}</value>)}
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <p>This is some info that's in the prompt</p>
        <textEntryInteraction responseIdentifier={identifier} expectedLength="15"/>
      </itemBody>
      {
        correctResponses.map(response =>
          <feedbackBlock outcomeIdentifier={s"responses.$identifier.value"} identifier={response}>
            <div class="feedback-block-correct">{correctFeedback}</div>
          </feedbackBlock>
        )
      }
      <feedbackBlock outcomeIdentifier={s"responses.$identifier.value"} incorrectResponse="true">
        <div class="feedback-block-incorrect">{incorrectFeedback}</div>
      </feedbackBlock>
    </assessmentItem>

  "TextEntryInteractionTransformer" should {

    val correctResponses = Seq("a", "b", "c!")

    val input = qti(
      correctResponses = correctResponses,
      correctFeedback = "That's correct!",
      incorrectFeedback = "Oops! Not right."
    )

    val componentsJson : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()
    new RuleTransformer(new TextEntryInteractionTransformer(componentsJson, input)).transform(input)

    val result = componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

    "return the correct component type" in {
      (result \ "componentType").as[String] must be equalTo "corespring-text-entry"
    }

    "return correct feedback" in {
      correctResponses.map(response => {
        (result \ "feedback").as[JsObject].keys.contains(response) must beTrue
        (result \ "feedback" \ response \ "correct").as[Boolean] must beTrue
      })
    }

    "return incorrect feedback" in {
      pending // not sure what the format of this should be?
    }

  }

}
