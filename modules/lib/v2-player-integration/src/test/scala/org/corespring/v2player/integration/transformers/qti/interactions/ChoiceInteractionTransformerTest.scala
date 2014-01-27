package org.corespring.v2player.integration.transformers.qti.interactions

import org.specs2.mutable.Specification
import play.api.libs.json.{Json, JsString, JsArray, JsObject}
import scala.collection.mutable
import scala.xml.transform.RuleTransformer
import scala.xml.{Elem, Node}
import org.corespring.v2player.integration.transformers.qti.interactions.ChoiceInteractionTransformer

class ChoiceInteractionTransformerTest extends Specification {

  def qti(rd: Elem, body: Elem): Node =
    <assessmentItem>
      <correctResponseFeedback>Default Correct</correctResponseFeedback>
      <incorrectResponseFeedback>Default Incorrect</incorrectResponseFeedback>
      {rd}<itemBody>
      {body}
    </itemBody>
    </assessmentItem>

  def responseDeclaration(cardinality: String, correctResponse: Elem) =
    <responseDeclaration identifier="Q_01" cardinality={cardinality} baseType="identifier">
      {correctResponse}
    </responseDeclaration>

  def choiceInteraction =
    <choiceInteraction responseIdentifier="Q_01" shuffle="false" maxChoices="1">
      <prompt>ITEM PROMPT?</prompt>
      <simpleChoice identifier="A">A
        <feedbackInline identifier="A" defaultFeedback="true"/>
      </simpleChoice>
      <simpleChoice identifier="B">B
        <feedbackInline identifier="B" defaultFeedback="true"/>
      </simpleChoice>
    </choiceInteraction>

  def inlineInteraction =
    <inlineChoiceInteraction responseIdentifier="Q_01" shuffle="false" maxChoices="1">
      <prompt>ITEM PROMPT?</prompt>
      <inlineChoice identifier="A"><math>A</math>
        <feedbackInline identifier="A" defaultFeedback="true"/>
      </inlineChoice>
      <inlineChoice identifier="B"><math>A</math>
        <feedbackInline identifier="B" defaultFeedback="true"/>
      </inlineChoice>
    </inlineChoiceInteraction>

  val singleChoice = qti(
    responseDeclaration("single", <correctResponse>
      <value>A</value>
    </correctResponse>),
    choiceInteraction)

  val inlineChoice = qti(
    responseDeclaration("single", <correctResponse>
      <value>A</value>
    </correctResponse>),
    inlineInteraction)

  val multipleChoice = qti(
    responseDeclaration("multiple", <correctResponse>
      <value>A</value> <value>B</value>
    </correctResponse>),
    choiceInteraction
  )


  "ChoiceInteractionTransformer" should {

    "transform choiceInteraction" in {

      val componentsJson = ChoiceInteractionTransformer.interactionJs(singleChoice)
      val q1 = componentsJson.get("Q_01").getOrElse(throw new RuntimeException("No component called Q_01"))

      (q1 \ "componentType").as[String] === "corespring-multiple-choice"
      (q1 \ "model" \ "config" \ "singleChoice" ).as[Boolean] === true
      ((q1 \ "model" \ "choices")(0) \ "label").as[String] === "A"
      (q1 \ "correctResponse" \ "value") === JsArray(Seq(JsString("A")))
      (q1 \ "feedback").as[Seq[JsObject]].length === 2
      ((q1 \ "feedback")(0) \ "value").as[String] === "A"
      ((q1 \ "feedback")(0) \ "feedback").as[String] === "Default Correct"
    }

    "transform inlineChoiceInteraction" in {

      val componentsJson : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()

      val out = new RuleTransformer(ChoiceInteractionTransformer).transform(inlineChoice)

      val q1 = componentsJson.get("Q_01").getOrElse(throw new RuntimeException("No component called Q_01"))

      (q1 \ "componentType").as[String] === "corespring-inline-choice"
      (q1 \ "model" \ "config" \ "singleChoice" ).as[Boolean] === true
      ((q1 \ "model" \ "choices")(0) \ "label").as[String] === "<math>A</math>"
      (q1 \ "correctResponse" \ "value") === JsArray(Seq(JsString("A")))
      (q1 \ "feedback").as[Seq[JsObject]].length === 2
      ((q1 \ "feedback")(0) \ "value").as[String] === "A"
      ((q1 \ "feedback")(0) \ "feedback").as[String] === "Default Correct"

    }
  }
}
