package org.corespring.qtiToV2.interactions

import org.corespring.qtiToV2.transformers.{ ItemTransformer, InteractionRuleTransformer }
import org.specs2.mutable.Specification

import scala.xml.transform.RuleTransformer

class ExtendedTextInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val expectedLength = 200
  val expectedLines = 5
  val maxStrings = 20
  val minStrings = 10

  "ExtendedTextInteractionTransformer" should {

    def qti =
      <assessmentItem>
        <responseDeclaration identifier={ identifier } cardinality="single" baseType="string"/>
        <itemBody>
          <extendedTextInteraction responseIdentifier={ identifier } expectedLength={ expectedLength.toString } expectedLines={ expectedLines.toString } maxStrings={ maxStrings.toString } minStrings={ minStrings.toString }></extendedTextInteraction>
        </itemBody>
      </assessmentItem>

    val componentsJson = ExtendedTextInteractionTransformer.interactionJs(qti, ItemTransformer.EmptyManifest)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

    val output = new InteractionRuleTransformer(ExtendedTextInteractionTransformer).transform(qti)

    val config = interactionResult \ "model" \ "config"

    "result must contain <corespring-extended-text-entry/>" in {
      (output \\ "corespring-extended-text-entry").find(n => (n \ "@id").text == identifier) must not beEmpty
    }

    "return the correct component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-extended-text-entry"
    }

    "return the correct expectedLength" in {
      (config \ "expectedLength").as[Int] must be equalTo expectedLength
    }

    "return the correct expectedLines" in {
      (config \ "expectedLines").as[Int] must be equalTo expectedLines
    }

    "return the correct maxStrings" in {
      (config \ "maxStrings").as[Int] must be equalTo maxStrings
    }

    "return the correct minStrings" in {
      (config \ "minStrings").as[Int] must be equalTo minStrings
    }

  }

  "transform" should {

    val prompt = "Hello, I'm a prompt!"

    def qti(prompt: String = prompt) =
      <assessmentItem>
        <responseDeclaration identifier="RESPONSE1" cardinality="single" baseType="string"/>
        <itemBody>
          <extendedTextInteraction responseIdentifier="RESPONSE1" expectedLength="5000">
            <prompt visible="true">{ prompt }</prompt>
          </extendedTextInteraction>
        </itemBody>
      </assessmentItem>

    val output = new InteractionRuleTransformer(ExtendedTextInteractionTransformer).transform(qti())

    "add a <div class='prompt'/> when it contains a prompt" in {
      val promptNode = (output \\ "p").find(p => (p \ "@class").text.contains("prompt"))
        .getOrElse(failure("Prompt not found"))
      promptNode.text must be equalTo (prompt)
    }

  }

}
