package org.corespring.qtiToV2.interactions

import org.corespring.qtiToV2.interactions.equation.DomainParser
import org.specs2.mutable.Specification

import scala.xml.Node
import scala.xml.transform.RuleTransformer

class TextEntryInteractionTransformerTest extends Specification with DomainParser {

  val identifier = "Q_01"
  val equationIdentifier = "Q_02"
  val lineIdentifier = "Q_03"

  def qti(correctResponses: Seq[String], correctFeedback: String, incorrectFeedback: String): Node =
    <assessmentItem>
      <responseDeclaration identifier={ identifier } cardinality="single" baseType="string">
        <correctResponse>
          { correctResponses.map(response => <value>{ response }</value>) }
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <p>This is some info that's in the prompt</p>
        <textEntryInteraction responseIdentifier={ identifier } expectedLength="15"/>
      </itemBody>
    </assessmentItem>

  def equationQti(equation: String, vars: String, domain: String, sigfigs: Int): Node = {
    val baseType = s"eqn: vars:$vars domain:$domain sigfigs:$sigfigs"
    <assessmentItem>
      <responseDeclaration identifier={ equationIdentifier } cardinality="single" baseType={ baseType }>
        <correctResponse>
          <value>{ equation }</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <p>This is some info that's in the prompt</p>
        <textEntryInteraction responseIdentifier={ equationIdentifier } expectedLength="15"/>
      </itemBody>
    </assessmentItem>
  }

  def lineQti(equation: String): Node = {
    <assessmentItem>
      <responseDeclaration identifier={ lineIdentifier } cardinality="single" baseType="line">
        <correctResponse>
          <value>{ equation }</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <p>This is some info that's in the prompt</p>
        <textEntryInteraction responseIdentifier={ lineIdentifier } expectedLength="15"/>
      </itemBody>
    </assessmentItem>
  }

  "TextEntryInteractionTransformer" should {

    val correctResponses = Seq("a", "b", "c")
    val correctFeedback = "That's correct!"
    val incorrectFeedback = "Oops! Not right."

    val input = qti(
      correctResponses = correctResponses,
      correctFeedback = correctFeedback,
      incorrectFeedback = incorrectFeedback)

    val interactionResult = TextEntryInteractionTransformer(input).interactionJs(input).get(identifier)
      .getOrElse(throw new RuntimeException(s"No component called $identifier"))

    val equation = "y=2x+7"
    val vars = "x,y"
    val domain = "-10->10,0"
    val sigfigs = 3

    val equationInput = equationQti(equation, vars, domain, sigfigs)
    val lineInput = lineQti(equation)

    val lineOutput = new RuleTransformer(new TextEntryInteractionTransformer(lineInput)).transform(lineInput)

    val equationInteractionResult = new TextEntryInteractionTransformer(equationInput).interactionJs(equationInput).get(equationIdentifier)
      .getOrElse(throw new RuntimeException(s"No component called $equationIdentifier"))

    new TextEntryInteractionTransformer(lineInput).interactionJs(lineInput).get(lineIdentifier)
      .getOrElse(throw new RuntimeException(s"No component called $lineIdentifier"))

    "return the correct interaction component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-text-entry"
      (equationInteractionResult \ "componentType").as[String] must be equalTo "corespring-function-entry"
    }

    "return the correct answers for the interaction" in {
      (interactionResult \ "correctResponse").as[Seq[String]] diff correctResponses must beEmpty
    }

    "returns the correct correct response vars" in {
      (equationInteractionResult \ "correctResponse" \ "vars").as[String] must be equalTo vars
    }

    "returns the correct correct response domain" in {
      (equationInteractionResult \ "correctResponse" \ "domain") must be equalTo parseDomain(domain)
    }

    "returns the correct correct response sigfigs" in {
      (equationInteractionResult \ "correctResponse" \ "sigfigs").as[Int] must be equalTo sigfigs
    }

    "returns the correct correct response equation" in {
      (equationInteractionResult \ "correctResponse" \ "equation").as[String] must be equalTo equation
    }

    "converts baseType=line to <corespring-function-entry/>" in {
      (lineOutput \\ "corespring-function-entry") must not beEmpty
    }

  }

}
