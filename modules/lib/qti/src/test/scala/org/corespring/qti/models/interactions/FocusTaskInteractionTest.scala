package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses.{ResponseOutcome, ArrayResponse}
import org.corespring.qti.models.{CorrectResponseMultiple, ResponseDeclaration}
import org.specs2.mutable._

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
      val rd = ResponseDeclaration("rid","multiple","identifier",Some(CorrectResponseMultiple(List("A", "B", "C"))),None)
      val response = ArrayResponse("rid", Seq("A"), Some(ResponseOutcome(0, false, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesBelowMin").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

    "too few selection is reflected in response object for checked" in {
      val rd = ResponseDeclaration("rid","multiple","identifier",Some(CorrectResponseMultiple(List("A", "B", "C"))),None)
      val response = ArrayResponse("rid", Seq("A"), Some(ResponseOutcome(0,false, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesBelowMin").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

    "too many selection is reflected in response object for non checked" in {
      val rd = ResponseDeclaration("rid","multiple","identifier",Some(CorrectResponseMultiple(List("A", "B", "C"))),None)
      val response = ArrayResponse("rid", Seq("A","B","C","D"), Some(ResponseOutcome(0,false, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesExceedMax").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

    "too many selection is reflected in response object for checked" in {
      val rd = ResponseDeclaration("rid","multiple","identifier",Some(CorrectResponseMultiple(List("A", "B", "C"))),None)
      val response = ArrayResponse("rid", Seq("A","B","C","D"), Some(ResponseOutcome(0,false, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesExceedMax").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

    "selection is correct if number of selected is correct if not checking for selection correctness" in {
      val rd = ResponseDeclaration("rid","multiple","identifier",Some(CorrectResponseMultiple(List("A", "B"))),None)
      val response = ArrayResponse("rid", Seq("C","D","E"), Some(ResponseOutcome(0,false, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome.score must equalTo(1.0)
    }

    "selection is correct if checking for selection correctness" in {
      val rd = ResponseDeclaration("rid","multiple","identifier",Some(CorrectResponseMultiple(List("A", "B","C"))),None)
      val response = ArrayResponse("rid", Seq("A","B"), Some(ResponseOutcome(0,false, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome.score must equalTo(1.0)

      val response2 = ArrayResponse("rid", Seq("A","B","C"), Some(ResponseOutcome(0,false, Some("Comment"))))
      val outcome2 = interactionChecked.getOutcome(Some(rd), response2).get
      outcome2.outcomeProperties.get("responsesCorrect").get must beTrue
      outcome2.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome2.score must equalTo(1.0)

      val response3 = ArrayResponse("rid", Seq("A","B","D"), Some(ResponseOutcome(0,false, Some("Comment"))))
      val outcome3 = interactionChecked.getOutcome(Some(rd), response3).get
      outcome3.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome3.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome3.score must equalTo(0.0)
    }

    "selection is incorrect if checking for selection correctness" in {
      val rd = ResponseDeclaration("rid","multiple","identifier",Some(CorrectResponseMultiple(List("A", "B","C"))),None)
      val response = ArrayResponse("rid", Seq("A","B","D"), Some(ResponseOutcome(0,false, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesIncorrect").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beTrue
      outcome.score must equalTo(0.0)
    }

    "selection is not incorrect if checking for selection correctness but response number is incorrect" in {
      val rd = ResponseDeclaration("rid","multiple","identifier",Some(CorrectResponseMultiple(List("A", "B","C"))),None)
      val response = ArrayResponse("rid", Seq("A"), Some(ResponseOutcome(0,false, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect").get must beFalse
      outcome.outcomeProperties.get("responsesIncorrect").get must beFalse
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beFalse
      outcome.score must equalTo(0.0)
    }

  }

}
