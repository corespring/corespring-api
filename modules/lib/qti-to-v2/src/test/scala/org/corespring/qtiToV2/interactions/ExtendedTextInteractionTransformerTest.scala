package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification

import scala.xml.transform.RuleTransformer

class ExtendedTextInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val expectedLength = 200
  val expectedLines = 5
  val maxStrings = 20
  val minStrings = 10

  def qti =
    <assessmentItem>
      <responseDeclaration identifier={ identifier } cardinality="single" baseType="string"/>
      <itemBody>
        <extendedTextInteraction responseIdentifier={ identifier } expectedLength={ expectedLength.toString } expectedLines={ expectedLines.toString } maxStrings={ maxStrings.toString } minStrings={ minStrings.toString }></extendedTextInteraction>
      </itemBody>
    </assessmentItem>

  "ExtendedTextInteractionTransformer" should {

    val componentsJson = ExtendedTextInteractionTransformer.interactionJs(qti)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

    val output = new RuleTransformer(ExtendedTextInteractionTransformer).transform(qti)

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

}
