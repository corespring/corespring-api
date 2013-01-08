package tests.qti.models.interactions

import org.specs2.mutable._
import qti.models.interactions.{FocusTaskInteraction, SelectTextInteraction}
import qti.models.{CorrectResponseMultiple, ResponseDeclaration}
import models.{ItemResponseOutcome, ArrayItemResponse}

class FocusTaskInteractionTest extends Specification {


  def interactionXml(checkCorrect:String) =
    <focusTaskInteraction responseIdentifier="rid" checkIfCorrect={checkCorrect} minSelections="2" maxSelections="3" shuffle="true">
        <prompt>Here is an item prompt which is asking the student to select FIVE of the elements below.</prompt>
        <focusChoice identifier="A">Option A</focusChoice>
        <focusChoice identifier="B">Option B</focusChoice>
        <focusChoice identifier="C">Option C</focusChoice>
        <focusChoice identifier="D" fixed="true">Option D</focusChoice>
        <focusChoice identifier="E">Option E</focusChoice>
        <focusChoice identifier="F">Option F</focusChoice>
    </focusTaskInteraction>

  "Focus Task Interaction" should {

    val interaction = FocusTaskInteraction(interactionXml("no"), None)
    val interactionChecked = FocusTaskInteraction(interactionXml("yes"), None)

    "parses" in {
      interaction mustNotEqual null
    }

    "too few selection is reflected in response object for non checked" in {
      val rd = ResponseDeclaration("rid","multiple",Some(CorrectResponseMultiple(List("A", "B", "C"))),None)
      val response = ArrayItemResponse("rid", Seq("A"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesBelowMin").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

    "too few selection is reflected in response object for checked" in {
      val rd = ResponseDeclaration("rid","multiple",Some(CorrectResponseMultiple(List("A", "B", "C"))),None)
      val response = ArrayItemResponse("rid", Seq("A"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesBelowMin").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

    "too many selection is reflected in response object for non checked" in {
      val rd = ResponseDeclaration("rid","multiple",Some(CorrectResponseMultiple(List("A", "B", "C"))),None)
      val response = ArrayItemResponse("rid", Seq("A","B","C","D"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesExceedMax").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

    "too many selection is reflected in response object for checked" in {
      val rd = ResponseDeclaration("rid","multiple",Some(CorrectResponseMultiple(List("A", "B", "C"))),None)
      val response = ArrayItemResponse("rid", Seq("A","B","C","D"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesExceedMax").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

    "selection is correct if number of selected is correct if not checking for selection correctness" in {
      val rd = ResponseDeclaration("rid","multiple",Some(CorrectResponseMultiple(List("A", "B"))),None)
      val response = ArrayItemResponse("rid", Seq("C","D","E"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome.score must equalTo(1.0)
    }

    "selection is correct if checking for selection correctness" in {
      val rd = ResponseDeclaration("rid","multiple",Some(CorrectResponseMultiple(List("A", "B","C"))),None)
      val response = ArrayItemResponse("rid", Seq("A","B"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome.score must equalTo(1.0)

      val response2 = ArrayItemResponse("rid", Seq("A","B","C"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome2 = interactionChecked.getOutcome(Some(rd), response2).get
      outcome2.outcomeProperties.get("responsesCorrect").get must beTrue
      outcome2.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome2.score must equalTo(1.0)

      val response3 = ArrayItemResponse("rid", Seq("A","B","D"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome3 = interactionChecked.getOutcome(Some(rd), response3).get
      outcome3.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome3.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome3.score must equalTo(0.0)
    }

    "selection is incorrect if checking for selection correctness" in {
      val rd = ResponseDeclaration("rid","multiple",Some(CorrectResponseMultiple(List("A", "B","C"))),None)
      val response = ArrayItemResponse("rid", Seq("A","B","D"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesIncorrect").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome.score must equalTo(0.0)
    }

    "selection is not incorrect if checking for selection correctness but response number is incorrect" in {
      val rd = ResponseDeclaration("rid","multiple",Some(CorrectResponseMultiple(List("A", "B","C"))),None)
      val response = ArrayItemResponse("rid", Seq("A"), Some(ItemResponseOutcome(0, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesIncorrect").get must beFalse
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

  }

}
