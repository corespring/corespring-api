package org.corespring.qtiToV2.interactions

import scala.xml.transform.RuleTransformer

import org.specs2.mutable.Specification
import play.api.libs.json._

class LineInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val anotherIdentifier = "Q_02"

  val correctResponse = "y=2x+7"
  val initialValues = Seq("-3,1", "-2,3")
  val scale = 2
  val domain = 10
  val range = 10
  val sigfigs = 3
  val domainLabel = "domain"
  val rangeLabel = "range"
  val tickLabelFrequency = 5

  def qti(correctResponse: String) =
    <assessmentItem>
      <responseDeclaration identifier={ identifier }>
        <correctResponse>
          <value>{ correctResponse }</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <lineInteraction jsxgraphcore="" responseIdentifier={ identifier } graph-width="300px" graph-height="300px" domain={ domain.toString } range={ range.toString } scale={ scale.toString } domain-label={ domainLabel } range-label={ rangeLabel } tick-label-frequency={ tickLabelFrequency.toString } sigfigs={ sigfigs.toString }>
          <graphline>
            { initialValues.map(v => <point>{ v }</point>) }
          </graphline>
        </lineInteraction>
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
        <lineInteraction responseIdentifier={ anotherIdentifier }/>
      </itemBody>
    </assessmentItem>

  def qtiNoResponseDeclaration(locked: Boolean) = <assessmentItem>
                                                    <itemBody>
                                                      <lineInteraction locked={ locked.toString } responseIdentifier={ anotherIdentifier }/>
                                                    </itemBody>
                                                  </assessmentItem>

  "LineInteractionTransformer" should {

    def json(m: Map[String, JsValue], id: String) = m.get(id).getOrElse {
      throw new RuntimeException(s"No json for: $id")
    }

    val input = qti(correctResponse)
    val output = new RuleTransformer(LineInteractionTransformer).transform(input)
    val interactionResult = json(LineInteractionTransformer.interactionJs(input), identifier)
    val noConfigInteractionResult = json(LineInteractionTransformer.interactionJs(qtiNoConfig), anotherIdentifier)

    val config = (interactionResult \ "model" \ "config")
    val noConfig = (noConfigInteractionResult \ "model" \ "config")

    "returns an object with no correctResponse if the interaction is locked and has no responseDeclaration" in {
      val jsonResult = json(LineInteractionTransformer.interactionJs(qtiNoResponseDeclaration(true)), anotherIdentifier)
      (jsonResult \ "correctResponse").asOpt[JsObject] === None
    }

    "throws an exception if the interaction is not locked and there is no responseDeclaration" in {
      LineInteractionTransformer.interactionJs(qtiNoResponseDeclaration(false)) must throwA[IllegalArgumentException]
    }

    "return the correct component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-line"
    }

    "returns correct response equation" in {
      (interactionResult \ "correctResponse").as[String] must be equalTo correctResponse
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

    "returns correct initial values" in {
      (noConfig \ "initialValues") must haveClass[JsUndefined]
      (config \ "initialValues").as[Seq[String]] diff initialValues must beEmpty
    }

    "removes all <lineInteraction/> elements" in {
      output \\ "lineInteraction" must beEmpty
    }

    "showInputs is always true" in {
      (noConfig \ "showInputs").as[JsBoolean].value must beTrue
      (config \ "showInputs").as[JsBoolean].value must beTrue
    }

    "exhibitOnly is always false" in {
      (config \ "exhibitOnly").as[JsBoolean].value must beFalse
      (config \ "exhibitOnly").as[JsBoolean].value must beFalse
    }

  }

}
