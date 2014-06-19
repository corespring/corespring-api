package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification
import play.api.libs.json._

import scala.xml.transform.RuleTransformer

class PointInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val anotherIdentifier = "Q_02"

  val correctResponses = Seq("0,1", "1,0")
  val pointLabels = Seq("A", "B")
  val maxPoints = 2
  val scale = 2
  val domain = 10
  val range = 10
  val sigfigs = 3
  val domainLabel = "domain"
  val rangeLabel = "range"
  val tickLabelFrequency = 5
  val showInputs = "true"

  def qti(correctResponses: Seq[String]) =
    <assessmentItem>
      <responseDeclaration identifier={ identifier }>
        <correctResponse>
          {
            correctResponses.map(response => <value>{ response }</value>)
          }
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <pointInteraction responseIdentifier={ identifier } point-labels={ pointLabels.mkString(",") } max-points={ maxPoints.toString } scale={ scale.toString } domain={ domain.toString } range={ range.toString } sigfigs={ sigfigs.toString } domain-label={ domainLabel } range-label={ rangeLabel } tick-label-frequency={ tickLabelFrequency.toString } show-inputs={ showInputs } locked="great"/>
      </itemBody>
    </assessmentItem>

  val qtiNoConfig =
    <assessmentItem>
      <responseDeclaration identifier={ anotherIdentifier }>
        <correctResponse>
          <value>don't care</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <pointInteraction responseIdentifier={ anotherIdentifier }/>
      </itemBody>
    </assessmentItem>

  "PointInteractionTransformer" should {

    val input = qti(correctResponses)
    val output = new RuleTransformer(PointInteractionTransformer).transform(input)

    val interactionResult =
      PointInteractionTransformer.interactionJs(input).get(identifier)
        .getOrElse(throw new RuntimeException(s"No component called $identifier"))

    val noConfigInteractionResult = PointInteractionTransformer.interactionJs(qtiNoConfig).get(anotherIdentifier)
      .getOrElse(throw new RuntimeException(s"No component called $anotherIdentifier"))

    val config = (interactionResult \ "model" \ "config")
    val noConfig = (noConfigInteractionResult \ "model" \ "config")

    "return the correct component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-point-intercept"
    }

    "returns correct response" in {
      (interactionResult \ "correctResponse").as[Seq[String]] diff correctResponses must beEmpty
    }

    "returns correct point labels" in {
      (noConfig \ "pointLabels") must haveClass[JsUndefined]
      (config \ "pointLabels").as[String] must be equalTo pointLabels.mkString(",")
    }

    "returns correct max points" in {
      (noConfig \ "maxPoints") must haveClass[JsUndefined]
      (config \ "maxPoints").as[JsString].value.toInt must be equalTo maxPoints
    }

    "returns correct scale" in {
      (noConfig \ "scale") must haveClass[JsUndefined]
      (config \ "scale").as[JsNumber].value.toInt must be equalTo scale
    }

    "returns correct domain" in {
      (noConfig \ "domain") must haveClass[JsUndefined]
      (config \ "domain").as[JsNumber].value.toInt must be equalTo domain
    }

    "returns correct range" in {
      (noConfig \ "range") must haveClass[JsUndefined]
      (config \ "range").as[JsNumber].value.toInt must be equalTo range
    }

    "returns correct sigfigs" in {
      (noConfig \ "sigfigs") must haveClass[JsUndefined]
      (config \ "sigfigs").as[JsNumber].value.toInt must be equalTo sigfigs
    }

    "returns correct domain label" in {
      (noConfig \ "domainLabel") must haveClass[JsUndefined]
      (config \ "domainLabel").as[JsString].value must be equalTo domainLabel
    }

    "returns correct range label" in {
      (noConfig \ "rangeLabel") must haveClass[JsUndefined]
      (config \ "rangeLabel").as[JsString].value must be equalTo rangeLabel
    }

    "returns correct tick label frequency" in {
      (noConfig \ "tickLabelFrequency") must haveClass[JsUndefined]
      (config \ "tickLabelFrequency").as[JsNumber].value.toInt must be equalTo tickLabelFrequency
    }

    "returns correct show inputs" in {
      (noConfig \ "showInputs") must haveClass[JsUndefined]
      (config \ "showInputs").as[JsString].value must be equalTo showInputs
    }

    "returns correct locked" in {
      (noConfig \ "locked") must haveClass[JsUndefined]
      (config \ "locked").as[JsBoolean].value must beTrue
    }

    "removes all <pointInteraction/> elements" in {
      output \\ "pointInteraction" must beEmpty
    }

  }

}
