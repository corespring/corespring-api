package org.corespring.v2player.integration.transformers.qti.interactions

import org.corespring.qtiToV2.interactions.FeedbackBlockTransformer
import org.specs2.mutable.Specification
import scala.xml.Node
import play.api.libs.json.{ Json, JsObject }
import scala.xml.transform.RuleTransformer
import org.specs2.execute.Failure

class FeedbackBlockTransformerTest extends Specification {

  val identifier = "Q_01"
  val anotherIdentifier = "Q_02"

  def feedbackIdentifier(identifier: String) = s"${identifier}_feedback"
  def feedbackIdentifier(identifier: String, outcomeIdentifier: String) = s"${identifier}_feedback_${outcomeIdentifier}"

  def qti(correctResponses: Seq[String], correctFeedback: String, incorrectFeedback: String): Node =
    <assessmentItem>
      <responseDeclaration identifier={ identifier } cardinality="single" baseType="string">
        <correctResponse>
          { correctResponses.map(response => <value>{ response }</value>) }
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <p>This is some info that's in the prompt</p>
        {
          Seq(identifier, anotherIdentifier).map(id => {
            Seq(
              <textEntryInteraction responseIdentifier={ id } expectedLength="15"/>,
              correctResponses.map(response =>
                <feedbackBlock outcomeIdentifier={ s"responses.$id.value" } identifier={ response }>
                  <div class="feedback-block-correct">{ s"$correctFeedback $id" }</div>
                </feedbackBlock>),
              <feedbackBlock outcomeIdentifier={ s"responses.$id.value" } incorrectResponse="true">
                <div class="feedback-block-incorrect">{ s"$incorrectFeedback $id" }</div>
              </feedbackBlock>)
          })
        }
      </itemBody>
    </assessmentItem>

  def outcomeSpecificQti(correctResponses: Seq[String], correctFeedback: String, incorrectFeedback: String): Node =
    <assessmentItem>
      <responseDeclaration identifier={ identifier } cardinality="single" baseType="string">
        <correctResponse>
          { correctResponses.map(response => <value>{ response }</value>) }
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <p>This is some info that's in the prompt</p>
        <textEntryInteraction responseIdentifier={ identifier } expectedLength="15"/>
        {
          correctResponses.map(response =>
            <feedbackBlock outcomeIdentifier={ s"responses.$identifier.outcome.$response" } identifier={ response } incorrectResponse="false">
              <div class="feedback-block-correct">{ correctFeedback }</div>
            </feedbackBlock>)
        }
      </itemBody>
    </assessmentItem>

  "FeedbackBlockTransformer" should {

    val correctResponses = Seq("a", "b", "c")
    val correctFeedback = "That's correct!"
    val incorrectFeedback = "Oops! Not right."

    val input = qti(
      correctResponses = correctResponses,
      correctFeedback = correctFeedback,
      incorrectFeedback = incorrectFeedback)

    val outcomeSpecificInput = outcomeSpecificQti(
      correctResponses = correctResponses,
      correctFeedback = correctFeedback,
      incorrectFeedback = incorrectFeedback)

    val componentsJson = FeedbackBlockTransformer.interactionJs(input) ++ FeedbackBlockTransformer.interactionJs(outcomeSpecificInput)
    val output = new RuleTransformer(FeedbackBlockTransformer(input)).transform(input)
    val outcomeSpecificOutput =
      new RuleTransformer(FeedbackBlockTransformer(outcomeSpecificInput)).transform(outcomeSpecificInput)

    def feedbackResult(identifier: String, outcomeIdentifier: String = null): JsObject =
      Option(outcomeIdentifier) match {
        case Some(outcomeIdentifier) => componentsJson.get(feedbackIdentifier(identifier, outcomeIdentifier))
          .getOrElse(throw new RuntimeException(s"No outcome feedback component for $outcomeIdentifier and $identifier"))
        case None => componentsJson.get(feedbackIdentifier(identifier))
          .getOrElse(throw new RuntimeException(s"No feedback component for $identifier"))
      }

    "return the correct feedback component type" in {
      (feedbackResult(identifier) \ "componentType").as[String] must be equalTo "corespring-feedback-block"
      correctResponses.map(correctResponse => {
        (feedbackResult(identifier, correctResponse) \ "componentType").as[String] must be equalTo "corespring-feedback-block"
      })
    }

    "return component weight as 0" in {
      (feedbackResult(identifier) \ "weight").as[Int] must be equalTo 0
    }

    "return correct feedback for answers" in {
      correctResponses.map(response => {
        (feedbackResult(identifier) \ "feedback" \ "correct").as[JsObject].keys.contains(response) must beTrue
      })
    }

    "return feedback text for answers with correct identifier" in {
      Seq(identifier, anotherIdentifier).map(id => {
        correctResponses.map(response => {
          (feedbackResult(id) \ "feedback" \ "correct").as[JsObject]
            .value(response).as[String] must be equalTo s"$correctFeedback $id"
        })
      }).flatten
    }

    "return correct outcome feedback" in {
      correctResponses.map(response => {
        val feedback = (feedbackResult(identifier, response) \ "feedback" \ "outcome" \ response)
        (outcomeSpecificInput \\ "feedbackBlock").find(f => (f \ "@outcomeIdentifier").text == s"responses.$identifier.outcome.$response") match {
          case Some(feedbackNode) => {
            (feedbackNode \ "@incorrectResponse").text match {
              case "true" => (feedback \ "correct").as[Boolean] must beFalse
              case _ => (feedback \ "correct").as[Boolean] must beTrue
            }
            feedbackNode.child.mkString must be equalTo (feedback \ "text").as[String]
          }
          case _ => Failure(s"Could not find feedback node for ${s"responses.$identifier.outcome.$response"}")
        }
      })
      success
    }

    "return incorrect feedback" in {
      Seq(identifier, anotherIdentifier).map(id => {
        (feedbackResult(id) \ "feedback" \ "incorrect").as[JsObject].keys.contains("*") must beTrue
        (feedbackResult(id) \ "feedback" \ "incorrect")
          .as[JsObject].value("*").as[String] must be equalTo s"$incorrectFeedback $id"
      })
    }

    "replace all <feedbackBlock/>s with a <corespring-feedback-block/>" in {
      val finalOutput = FeedbackBlockTransformer.transform(output.head)
      (finalOutput \ "feedbackBlock").toSeq.length must be equalTo 0
      (finalOutput \\ "corespring-feedback-block").toSeq match {
        case seq if seq.isEmpty => failure("Output did not contain corespring-feedback-block")
        case seq => (seq.head \\ "@id").text must be equalTo feedbackIdentifier(identifier)
      }

      val finalOutcomeSpecificOutput = FeedbackBlockTransformer.transform(outcomeSpecificOutput.head)

      (finalOutcomeSpecificOutput \ "feedbackBlock").toSeq.length must be equalTo 0
      (finalOutcomeSpecificOutput \\ "corespring-feedback-block").toSeq match {
        case seq if seq.isEmpty => failure("Output did not contain corespring-feedback-block")
        case seq =>
          correctResponses.map(r => s"${identifier}_feedback_${r}").toSeq diff
            seq.map(n => (n \ "@id").text) must beEmpty
      }
    }

    "contain unique <corespring-feedback-block/> by id" in {
      Seq(identifier, anotherIdentifier).map(id => {
        (FeedbackBlockTransformer.transform(output.head) \\ "corespring-feedback-block")
          .filter(n => (n \ "@id").text == feedbackIdentifier(id)).toSeq.length must be equalTo 1
      })
    }

    "identifies correct feedback for <div class='feedback-block-correct'/>'" in {
      def qti(response: String, feedback: String) =
        <assessmentItem>
          <responseDeclaration identifier="Q_01" cardinality="single" baseType="string">
            <correctResponse>
              <value>response</value>
            </correctResponse>
          </responseDeclaration>
          <itemBody>
            <textEntryInteraction responseIdentifier="Q_01" expectedLength="15"/>
            <feedbackBlock outcomeIdentifier="responses.Q_01.value" identifier={ response }>
              <div class="feedback-block-correct">{ feedback }</div>
            </feedbackBlock>
          </itemBody>
        </assessmentItem>

      val response = "a"
      val feedback = "feedback"

      val input = qti(response, feedback)

      val json = new FeedbackBlockTransformer(input).interactionJs(input).get("Q_01_feedback")
        .getOrElse(throw new RuntimeException(s"No feedback component for Q_01"))

      (json \ "feedback" \ "correct" \ response).asOpt[String] match {
        case Some(text) => text must be equalTo feedback
        case None => failure("Json did not contain feedback for response")
      }
    }

  }

}
