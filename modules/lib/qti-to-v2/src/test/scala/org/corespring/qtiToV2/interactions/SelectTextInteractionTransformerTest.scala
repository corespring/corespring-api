package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification

import scala.xml.XML
import scala.xml.transform.RuleTransformer
import play.api.libs.json.{JsBoolean, JsString, JsObject}
import scala.util.matching.Regex

class SelectTextInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val checkIfCorrect = "no"
  val minSelections = 2
  val maxSelections = 10

  def qti(selectionText: String, selectionType: String) = XML.loadString(s"""
    <assessmentItem>
      <responseDeclaration identifier="${identifier}">
      </responseDeclaration>
      <itemBody>
        <selectTextInteraction responseIdentifier="${ identifier }" selectionType="${ selectionType }" checkIfCorrect="${ checkIfCorrect }" minSelections="${ minSelections.toString }" maxSelections="${ maxSelections.toString }">${ selectionText }"</selectTextInteraction>
      </itemBody>
    </assessmentItem>
    """)

  val selectionTextWord =
    """Lorem <b><i>ipsum</i></b> <correct>dolor</correct> sit amet, consectetur adipisicing <correct>elit</correct>, sed do eiusmod tempor incididunt ut
       labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut
       aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
       fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit
       anim id est laborum."""

  val selectionTextSentence =
    """<correct>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut
       labore et dolore magna aliqua</correct>. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut
       aliquip ex ea commodo consequat. <correct>Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
       fugiat nulla pariatur</correct>. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit
       anim id est laborum."""

  "SelectTextInteractionTransformer for word based select text" should {

    val input = qti(selectionTextWord, "word")
    val componentsJson = SelectTextInteractionTransformer.interactionJs(input)
    val output = new RuleTransformer(SelectTextInteractionTransformer).transform(input)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

    val config = interactionResult \ "model" \ "config"
    val choices = interactionResult \ "model" \ "choices"

    "return the correct interaction component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-select-text"
    }
    "return the correct choices" in {
      val choicesSeq = choices.as[Seq[JsObject]]
      val correctChoices = List(2, 7)
      List.range(0, choicesSeq.length).diff(correctChoices).foreach(e => choicesSeq(e) \ "correct" must not be equalTo(JsBoolean(true)))
      correctChoices.foreach(e => choicesSeq(e) \ "correct" must be equalTo(JsBoolean(true)))
      choicesSeq(0) \ "data" must be equalTo(JsString("Lorem"))
      choicesSeq(1) \ "data" must be equalTo(JsString("<b><i>ipsum</i></b>"))
      choicesSeq(2) \ "data" must be equalTo(JsString("dolor"))
    }


    "return the correct config selectionUnit value" in {
      (config \ "selectionUnit").as[String] must be equalTo "word"
    }

    "return the correct config checkIfCorrect value" in {
      (config \ "checkIfCorrect").as[String] must be equalTo checkIfCorrect
    }

    "return the correct minSelections value" in {
      (config \ "minSelections").as[Int] must be equalTo minSelections
    }

    "return the correct maxSelections value" in {
      (config \ "maxSelections").as[Int] must be equalTo maxSelections
    }

    "should remove <selectTextInteraction/>" in {
      (output \\ "selectTextInteraction") must beEmpty
    }

    "should add <corespring-select-text />" in {
      (output \\ "corespring-select-text").find(n => (n \ "@id").text == identifier) must not beEmpty
    }

    "should produce <corespring-select-text/>" in {
      val children = (output \\ "corespring-select-text").find(n => (n \ "@id").text == identifier)
      children must not beEmpty
    }
  }

  "SelectTextInteractionTransformer for sentence based select text" should {

    val input = qti(selectionTextSentence, "sentence")
    val componentsJson = SelectTextInteractionTransformer.interactionJs(input)
    val output = new RuleTransformer(SelectTextInteractionTransformer).transform(input)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

    val config = interactionResult \ "model" \ "config"
    val choices = interactionResult \ "model" \ "choices"

    "return the correct interaction component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-select-text"
    }
    "return the correct choices" in {
      val choicesSeq = choices.as[Seq[JsObject]]
      val correctChoices = List(0, 2)
      List.range(0, choicesSeq.length).diff(correctChoices).foreach(e => choicesSeq(e) \ "correct" must not be equalTo(JsBoolean(true)))
      correctChoices.foreach(e => choicesSeq(e) \ "correct" must be equalTo(JsBoolean(true)))
      choicesSeq(0) \ "data" must be equalTo(JsString("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut\n       labore et dolore magna aliqua."))
    }

    "return the correct config selectionUnit value" in {
      (config \ "selectionUnit").as[String] must be equalTo "sentence"
    }

    "return the correct config checkIfCorrect value" in {
      (config \ "checkIfCorrect").as[String] must be equalTo checkIfCorrect
    }

    "return the correct minSelections value" in {
      (config \ "minSelections").as[Int] must be equalTo minSelections
    }

    "return the correct maxSelections value" in {
      (config \ "maxSelections").as[Int] must be equalTo maxSelections
    }

    "should remove <selectTextInteraction/>" in {
      (output \\ "selectTextInteraction") must beEmpty
    }

    "should add <corespring-select-text />" in {
      (output \\ "corespring-select-text").find(n => (n \ "@id").text == identifier) must not beEmpty
    }

    "should produce <corespring-select-text/>" in {
      val children = (output \\ "corespring-select-text").find(n => (n \ "@id").text == identifier)
      children must not beEmpty
    }

  }

}
