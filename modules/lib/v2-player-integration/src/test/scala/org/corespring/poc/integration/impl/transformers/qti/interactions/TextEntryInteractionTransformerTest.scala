package org.corespring.poc.integration.impl.transformers.qti.interactions

import org.specs2.mutable.Specification
import scala.xml.{XML, Node}
import scala.collection.mutable
import play.api.libs.json.{Json, JsObject}
import scala.xml.transform.RuleTransformer

class TextEntryInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val feedbackIdentifier = s"${identifier}_feedback"

  def qti(correctResponses: Seq[String], correctFeedback: String, incorrectFeedback: String): Node =
    XML.loadString(<assessmentItem>
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
    </assessmentItem>.toString)

  "TextEntryInteractionTransformer" should {

    val correctResponses = Seq("a", "b", "c")
    val correctFeedback = "That's correct!"
    val incorrectFeedback = "Oops! Not right."

    val input = qti(
      correctResponses = correctResponses,
      correctFeedback = correctFeedback,
      incorrectFeedback = incorrectFeedback
    )

    val componentsJson : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()
    new RuleTransformer(new TextEntryInteractionTransformer(componentsJson, input)).transform(input)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))
    val feedbackResult = componentsJson.get(feedbackIdentifier)
      .getOrElse(throw new RuntimeException("No feedback component for $identifier"))

    "return the correct interaction component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-text-entry"
    }

    "return the correct answers for the interaction" in {
      (interactionResult \ "correctResponse").as[Seq[String]] diff correctResponses must beEmpty
    }

    "return the correct feedback component type" in {
      (feedbackResult \ "componentType").as[String] must be equalTo "corespring-feedback-block"
    }

    "return correct feedback for answers" in {
      correctResponses.map(response => {
        (feedbackResult \ "feedback" \ "correct").as[JsObject].keys.contains(response) must beTrue
      })
    }

    "return correct feedback text for answers" in {
      correctResponses.map(response => {
        (feedbackResult \ "feedback" \ "correct").as[JsObject]
          .value(response).as[String] must be equalTo correctFeedback
      })
    }

    "return incorrect feedback" in {
      (feedbackResult \ "feedback" \ "incorrect").as[JsObject].keys.contains("*") must beTrue
      (feedbackResult \ "feedback" \ "incorrect").as[JsObject].value("*").as[String] must be equalTo incorrectFeedback
    }

  }

}
