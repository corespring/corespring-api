package tests.qti.models.interactions

import org.specs2.mutable._
import qti.models.interactions.SelectTextInteraction
import qti.models.{CorrectResponseMultiple, ResponseDeclaration}
import play.libs.Json
import org.corespring.platform.core.models.itemSession.{ItemResponseOutcome, ArrayItemResponse}

class SelectTextInteractionTest extends Specification {


  val sentenceInteractionXml =
  <selectTextInteraction responseIdentifier="selectText" selectionType="sentence" minSelections="2" maxSelections="2">
    "It turns out my mother loved the name Ruth.
    <correct>That's how I got my name and how my father got these: he let Ty Cobb name me after Babe Ruth.</correct>
    "
    <correct>I tried to swallow Vikram S. Pandit but couldn't</correct>.
  </selectTextInteraction>

  val sentenceInteractionWithCorrectCheckXml =
  <selectTextInteraction responseIdentifier="selectText" selectionType="sentence" checkIfCorrect="yes" minSelections="2" maxSelections="2">
    "It turns out my mother loved the name Ruth.
    <correct>That's how I got my name and how my father got these: he let Ty Cobb name me after Babe Ruth.</correct>
    "
    <correct>I tried to swallow Vikram S. Pandit but couldn't</correct>.
  </selectTextInteraction>

  val wordInteractionXml =
  <selectTextInteraction responseIdentifier="selectText" selectionType="word" minSelections="3" maxSelections="3">
    "It turns out my mother loved the name Ruth.
    <correct>That's</correct> how I got my name and how my father got these: he let Ty Cobb name me after Babe Ruth.
    "
    I tried to <correct>swallow</correct> Vikram S. Pandit but couldn't.
  </selectTextInteraction>


  "Select Text Interaction" should {

    val interaction = SelectTextInteraction(sentenceInteractionXml, None)
    val interactionChecked = SelectTextInteraction(sentenceInteractionWithCorrectCheckXml, None)

    "parses" in {
      interaction mustNotEqual null
      interaction.responseIdentifier must equalTo("selectText")
      if (interaction.maxSelection == 2) success else failure("Maximum selection should be 2")
      if (interaction.minSelection == 2) success else failure("Minimum selection should be 2")
    }

    "tags sentences correctly" in {
      val taggedXml = SelectTextInteraction.preProcessXml(sentenceInteractionXml)
      (taggedXml \ "span" filterNot (_ \ "@id" isEmpty)).size must equalTo(3)
    }

    "tags words correctly" in {
      val taggedXml = SelectTextInteraction.preProcessXml(wordInteractionXml)
      (taggedXml \ "span" filterNot (_ \ "@id" isEmpty)).size must equalTo(39)
    }

    "validates" in {
      interaction.validate(null) must equalTo((true, "Ok"))
    }

    "too few selection is reflected in response object" in {
      val rd = ResponseDeclaration("selectText","multiple","identifier",Some(CorrectResponseMultiple(List("2", "3", "7"))),None)
      val response = ArrayItemResponse("selectText", Seq("3"), Some(ItemResponseOutcome(0,false, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesBelowMin").get must beTrue
    }

    "too many selection is reflected in response object" in {
      val rd = ResponseDeclaration("selectText","multiple","identifier",Some(CorrectResponseMultiple(List("2", "3"))),None)
      val response = ArrayItemResponse("selectText", Seq("3","4","5","6"), Some(ItemResponseOutcome(0,false, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesExceedMax").get must beTrue
    }

    "correct selection is reflected in response object" in {
      val rd = ResponseDeclaration("selectText","multiple","identifier",Some(CorrectResponseMultiple(List("2", "3"))),None)
      val response = ArrayItemResponse("selectText", Seq("2","3"), Some(ItemResponseOutcome(0,false, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect").get must beTrue
    }

    "if no correctness checking then response should be correct regardless of answer" in {
      val rd = ResponseDeclaration("selectText","multiple","identifier",Some(CorrectResponseMultiple(List("2", "3"))),None)
      val response = ArrayItemResponse("selectText", Seq("1","4"), Some(ItemResponseOutcome(0,false, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect").get must beTrue
    }

    "correct number of selection is reflected in response object" in {
      val rd = ResponseDeclaration("selectText","multiple","identifier",Some(CorrectResponseMultiple(List("2", "3"))),None)
      val response = ArrayItemResponse("selectText", Seq("2","3"), Some(ItemResponseOutcome(0,false, Some("Comment"))))
      val outcome = interaction.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beTrue
    }

    "correct number of selection with some incorrect anwsers is reflected in response object" in {
      val rd = ResponseDeclaration("selectText","multiple","identifier",Some(CorrectResponseMultiple(List("2", "3"))),None)
      val response = ArrayItemResponse("selectText", Seq("2","4"), Some(ItemResponseOutcome(0,false, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect") must beNone
      outcome.outcomeProperties.get("responsesIncorrect").get must beTrue
      outcome.outcomeProperties.get("responsesNumberCorrect").get must beTrue
    }

    "incorrect selection is reflected in response object" in {
      val rd = ResponseDeclaration("selectText","multiple","identifier",Some(CorrectResponseMultiple(List("2", "3"))),None)
      val response = ArrayItemResponse("selectText", Seq("1","4"), Some(ItemResponseOutcome(0,false, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect") must beNone
      outcome.outcomeProperties.get("responsesIncorrect").get must beTrue
    }

    "incorrect selection and incorrent number of selection should both be reflected in response object" in {
      val rd = ResponseDeclaration("selectText","multiple","identifer",Some(CorrectResponseMultiple(List("2", "3"))),None)
      val response = ArrayItemResponse("selectText", Seq("1"), Some(ItemResponseOutcome(0,false, Some("Comment"))))
      val outcome = interactionChecked.getOutcome(Some(rd), response).get
      outcome.outcomeProperties.get("responsesCorrect") must beNone
      outcome.outcomeProperties.get("responsesIncorrect").get must beTrue
      outcome.outcomeProperties.get("responsesBelowMin").get must beTrue
    }

    "parse correct responses correctly" in {
      interaction.correctResponse.get.value
      interaction.correctResponse.get.value must beEqualTo(Seq("2", "3"))
    }

  }

}
