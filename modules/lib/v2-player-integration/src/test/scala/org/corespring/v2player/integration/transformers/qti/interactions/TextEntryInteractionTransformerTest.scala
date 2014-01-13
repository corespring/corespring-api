package org.corespring.v2player.integration.transformers.qti.interactions

import org.specs2.mutable.Specification
import scala.xml.{XML, Node}
import scala.collection.mutable
import play.api.libs.json.{Json, JsObject}
import scala.xml.transform.RuleTransformer
import org.corespring.v2player.integration.transformers.qti.interactions.TextEntryInteractionTransformer

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
    </assessmentItem>

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

    "return the correct interaction component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-text-entry"
    }

    "return the correct answers for the interaction" in {
      (interactionResult \ "correctResponse").as[Seq[String]] diff correctResponses must beEmpty
    }

  }

}
