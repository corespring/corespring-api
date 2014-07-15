package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification

import scala.xml.transform.RuleTransformer

class SelectTextInteractionTransformerTest extends Specification {

//  val identifier = "Q_01"
//
//  val selectionType = "sentence"
//  val checkIfCorrect = "no"
//  val minSelections = 2
//  val maxSelections = 10

//  def qti(selectionText: String) =
//    <assessmentItem>
//      <responseDeclaration identifier={ identifier }>
//      </responseDeclaration>
//      <itemBody>
//        <selectTextInteraction responseIdentifier={ identifier } selectionType={ selectionType } checkIfCorrect={ checkIfCorrect } minSelections={ minSelections.toString } maxSelections={ maxSelections.toString }>{ selectionText }</selectTextInteraction>
//      </itemBody>
//    </assessmentItem>
//
//  val selectionText =
//    """Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut
//       labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut
//       aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
//       fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit
//       anim id est laborum."""

  "SelectTextInteractionTransformer" should {

//    val input = qti(selectionText)
//    val componentsJson = SelectTextInteractionTransformer.interactionJs(input)
//    val output = new RuleTransformer(SelectTextInteractionTransformer).transform(input)
//
//    val interactionResult =
//      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))
//
//    val config = interactionResult \ "model" \ "config"

//    "return the correct interaction component type" in {
//      (interactionResult \ "componentType").as[String] must be equalTo "corespring-select-text"
//    }
//
//    "return the correct config selectionUnit value" in {
//      (config \ "selectionUnit").as[String] must be equalTo selectionType
//    }
//
//    "return the correct config checkIfCorrect value" in {
//      (config \ "checkIfCorrect").as[String] must be equalTo checkIfCorrect
//    }
//
//    "return the correct minSelections value" in {
//      (config \ "minSelections").as[Int] must be equalTo minSelections
//    }

    "do the things" in {
      val input = <assessmentItem>
        <itemBody>
          <selectTextInteraction responseIdentifier="selectText" selectionType="sentence" checkIfCorrect="yes" minSelections="1" maxSelections="1">
            Elizabeth now began to revive. But not long was the interval of tranquillity; for when supper was over, singing was talked of, and she had the mortification of seeing Mary, after very little entreaty, preparing to oblige the company. <correct>By many significant looks and silent entreaties, did she endeavour to prevent such a proof of complaisance, &#8212;but in vain; Mary would not understand them; such an opportunity of exhibiting was delightful to her, and she began her song.</correct> Elizabeth's eyes were fixed on her with most painful sensations; and she watched her progress through the several stanzas with an impatience which was very ill rewarded at their close; for Mary, on receiving amongst the thanks of the table, the hint of a hope that she might be prevailed on to favour them again, after the pause of half a minute began another. Mary's powers were by no means fitted for such a display; her voice was weak, and her manner affected. &#8212;Elizabeth was in agonies. She looked at Jane, to see how she bore it; but Jane was very composedly talking to Bingley. She looked at his two sisters, and saw them making signs of derision at each other, and at Darcy, who continued however imperturbably grave.
          </selectTextInteraction>
        </itemBody>
      </assessmentItem>

      val componentsJson = SelectTextInteractionTransformer.interactionJs(input)
      val output = new RuleTransformer(SelectTextInteractionTransformer).transform(input)

      true === false

    }

//    "return the correct maxSelections value" in {
//      (config \ "maxSelections").as[Int] must be equalTo maxSelections
//    }
//
//    "should remove <selectTextInteraction/>" in {
//      (output \\ "selectTextInteraction") must beEmpty
//    }
//
//    "should add <corespring-select-text />" in {
//      (output \\ "corespring-select-text").find(n => (n \ "@id").text == identifier) must not beEmpty
//    }
//
//    "should produce <corespring-select-text/> with all children of previous <selectTextInteraction/>" in {
//      val children = (output \\ "corespring-select-text").find(n => (n \ "@id").text == identifier).get.child
//      children must be equalTo (input \\ "selectTextInteraction").head.child
//    }

  }

}
