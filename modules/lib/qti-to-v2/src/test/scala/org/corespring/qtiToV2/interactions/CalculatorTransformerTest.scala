package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification

import scala.xml.transform.RuleTransformer

class CalculatorTransformerTest extends Specification {

  val identifier = "AUTO_INSERTED_CALCULATOR"
  val calculatorType = "basic"

  def qti =
    <assessmentItem>
      <itemBody>
        <csCalculator responseIdentifier={ identifier } type={ calculatorType }></csCalculator>
      </itemBody>
    </assessmentItem>

  "CalculatorTransformer" should {

    val componentsJson = CalculatorTransformer.interactionJs(qti)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

    val output = new RuleTransformer(CalculatorTransformer).transform(qti)

    val config = interactionResult \ "model" \ "config"

    "result must contain calculator tag" in {
      (output \\ "p" \\ "corespring-calculator").find(n => (n \ "@id").text == identifier) must not beEmpty
    }

    "return the correct component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-calculator"
    }

    "return the correct title" in {
      (interactionResult \ "title").as[String] must be equalTo "Calculator"
    }

    "return the correct isTool" in {
      (interactionResult \ "isTool").as[Boolean] must be equalTo true
    }

    "return the correct weight" in {
      (interactionResult \ "weight").as[Int] must be equalTo 0
    }

    "return the correct type" in {
      (config \ "type").as[String] must be equalTo calculatorType
    }

  }

}
