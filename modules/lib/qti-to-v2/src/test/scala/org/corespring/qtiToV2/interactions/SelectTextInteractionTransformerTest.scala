package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification

import scala.xml.transform.RuleTransformer
import play.api.libs.json.JsObject
import scala.util.matching.Regex

class SelectTextInteractionTransformerTest extends Specification {

  val identifier = "Q_01"

  val selectionType = "sentence"
  val checkIfCorrect = "no"
  val minSelections = 2
  val maxSelections = 10

  def qti(selectionText: String) =
    <assessmentItem>
      <responseDeclaration identifier={ identifier }>
      </responseDeclaration>
      <itemBody>
        <selectTextInteraction responseIdentifier={ identifier } selectionType={ selectionType } checkIfCorrect={ checkIfCorrect } minSelections={ minSelections.toString } maxSelections={ maxSelections.toString }>{ selectionText }</selectTextInteraction>
      </itemBody>
    </assessmentItem>

  val selectionText =
    """Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut
       labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut
       aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
       fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit
       anim id est laborum."""

  "SelectTextInteractionTransformer" should {

    val input = qti(selectionText)
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
      choices.as[Seq[JsObject]].map(choice => (choice \ "data").as[String]) diff TextSplitter.sentences(selectionText) must beEmpty
    }


    "return the correct config selectionUnit value" in {
      (config \ "selectionUnit").as[String] must be equalTo selectionType
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

    "should produce <corespring-select-text/> with all children of previous <selectTextInteraction/>" in {
      val children = (output \\ "corespring-select-text").find(n => (n \ "@id").text == identifier).get.child
      children must be equalTo (input \\ "selectTextInteraction").head.child
    }

  }

}
