package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification
import play.api.libs.json.{Json, JsObject, JsValue}

import scala.xml.Node
import scala.xml.transform.RuleTransformer

class SelectPointInteractionTransformerTest extends Specification {


  "points interaction" should {

    val prompt = "This is a prompt"
    val identifier = "RESPONSE1"
    val maxChoices = 3
    val correctResponses = Seq((2,1), (5,3), (8,5))
    val settings = Map("gridVisible" -> "1", "gridHeightInPixels" -> "600", "gridWidthInPixels" -> "600",
      "xAxisTitle" -> " ", "xAxisMinValue" -> "0", "xAxisMaxValue" -> "8", "xAxisStepValue" -> "1",
      "xAxisLabelPattern" -> "1", "yAxisTitle" -> "1", "yAxisMinValue" -> "0", "yAxisMaxValue" -> "8",
      "yAxisStepValue" -> "1", "yAxisLabelPattern" -> "1")

    def getSetting(string: String) = settings.get(string).getOrElse(throw new IllegalStateException(s"No setting for $string"))

    def qti(prompt: String = prompt, identifier: String = identifier, correctResponses: Seq[(Int,Int)] = correctResponses,
            settings: Map[String, String] = settings) =
      <assessmentItem>
        <responseDeclaration identifier={identifier} cardinality="record">
          <fieldValue identifier="xcoordinate" baseType="float"/>
          <fieldValue identifier="ycoordinate" baseType="float"/>
          <correctResponse>
            {
              correctResponses.map{ case(x,y) =>
                  <value xcoordinate={x.toString} ycoordinate={y.toString} tolerance ="0"/>
              }
            }
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <selectPointInteraction responseIdentifier={identifier} minChoices="0" maxChoices={maxChoices.toString} class="placePointsSelectPointInteraction">
            <prompt visible="true">{prompt}</prompt>
            <object>
              {
                settings.map{ case(name, value) =>
                    <param name={name} valuetype="DATA" value={value}/>
                }
              }
            </object>
          </selectPointInteraction>
        </itemBody>
      </assessmentItem>

    def transformer(qti: Node) = new SelectPointInteractionTransformer(qti)

    def output(qtiNode: Node = qti()) = new RuleTransformer(transformer(qtiNode)).transform(qtiNode)
    def jsonOutput(qtiNode: Node = qti(), identifier: String = identifier): JsObject = {
      transformer(qtiNode).interactionJs(qtiNode).get(identifier)
        .getOrElse(throw new IllegalStateException(s"Missing JSON for $identifier"))
    }

    "transform" should {

      "return <corespring-point-intercept/> node" in {
        (output() \\ "corespring-point-intercept").head must be equalTo(<corespring-point-intercept id={identifier} />)
      }

      "return <p class='prompt'/> containing prompt" in {
        (output() \\ "p").find(p => (p \ "@class").text.contains("prompt"))
          .getOrElse(throw new IllegalStateException("Contained no prompt!")).text must be equalTo(prompt)
      }

      "not return <object/>" in {
        (output() \\ "object").isEmpty must beTrue
      }

    }

    "interactionJs" should {

      "contain componentType = corespring-point-intercept" in {
        (jsonOutput() \ "componentType").as[String] must be equalTo("corespring-point-intercept")
      }

      "contain correctResponse" in {
        (jsonOutput() \ "correctResponse").as[Seq[String]] must be equalTo(correctResponses.map{ case (x,y) => s"$x,$y"})
      }

      "contain domainLabel" in {
        (jsonOutput() \ "model" \ "config" \ "domainLabel").as[String] must be equalTo(getSetting("xAxisTitle"))
      }

      "contain rangeLabel" in {
        (jsonOutput() \ "model" \ "config" \ "rangeLabel").as[String] must be equalTo(getSetting("yAxisTitle"))
      }

      "contain graphWidth" in {
        (jsonOutput() \ "model" \ "config" \ "graphWidth").as[String] must be equalTo(getSetting("gridWidthInPixels"))
      }

      "contain graphHeight" in {
        (jsonOutput() \ "model" \ "config" \ "graphHeight").as[String] must be equalTo(getSetting("gridHeightInPixels"))
      }

      "contain domain" in {
        (jsonOutput() \ "model" \ "config" \ "domain").as[Int] must be equalTo(getSetting("xAxisMaxValue").toInt)
      }

      "contain range" in {
        (jsonOutput() \ "model" \ "config" \ "range").as[Int] must be equalTo(getSetting("yAxisMaxValue").toInt)
      }

      "contain maxPoints" in {
        (jsonOutput() \ "model" \ "config" \ "maxPoints").as[Int] must be equalTo(maxChoices)
      }

    }

  }

  "line interaction" should {

    val identifier = "RESPONSE1"
    val prompt = "This is the prompt"
    val settings = Map("gridVisible" -> "1", "gridHeightInPixels" -> "300", "gridWidthInPixels" -> "300",
      "xAxisTitle" -> "t", "xAxisMinValue" -> "0", "xAxisMaxValue" -> "8", "xAxisStepValue" -> "1",
      "xAxisLabelPattern" -> "1", "yAxisTitle" -> "p", "yAxisMinValue" -> "0", "yAxisMaxValue" -> "160",
      "yAxisStepValue" -> "20", "yAxisLabelPattern" -> "1")

    val b = "0"
    val m = "10"

    def getSetting(string: String) = settings.get(string).getOrElse(throw new IllegalStateException(s"No setting for $string"))

    def qti(prompt: String = prompt, identifier: String = identifier, settings: Map[String, String] = settings,
            m: String = m, b: String = b) =
      <assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1" xmlns:m="http://www.w3.org/1998/Math/MathML" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd" title="663885" identifier="663885" adaptive="false" timeDependent="false">
        <responseDeclaration identifier={identifier} cardinality="record">
          <fieldValue identifier="startPointXCoordinate" baseType="float"/>
          <fieldValue identifier="startPointYCoordinate" baseType="float"/>
          <fieldValue identifier="endPointXCoordinate" baseType="float"/>
          <fieldValue identifier="endPointYCoordinate" baseType="float"/>
          <fieldValue identifier="XIntercept" baseType="float"/>
          <fieldValue identifier="YIntercept" baseType="float"/>
          <fieldValue identifier="Slope" baseType="float"/>
          <correctResponse>
            <value startPointXCoordinate="0" startPointYCoordinate="0" startPointConsider="1" startPointTolerance="0" endPointXCoordinate="8" endPointYCoordinate="80" endPointConsider="0" endPointTolerance="0" xIntercept="0" xInterceptConsider="0" xInterceptTolerance="0" yIntercept={b} yInterceptConsider="0" yInterceptTolerance="0" slope={m} slopeConsider="1" slopeTolerance="0.25"/>
          </correctResponse>
        </responseDeclaration>
        <outcomeDeclaration identifier="SCORE" cardinality="single" baseType="float">
          <defaultValue>
            <value>0</value>
          </defaultValue>
        </outcomeDeclaration>
        <itemBody>
          <selectPointInteraction responseIdentifier={identifier} minChoices="0" maxChoices="1" class="singleLineSelectPointInteraction">
            <prompt visible="true">{prompt}</prompt>
            <object>
              {
                settings.map{ case(name, value) =>
                    <param name={name} valuetype="DATA" value={value}/>
                }
              }
            </object>
          </selectPointInteraction>
        </itemBody>
      </assessmentItem>

    def transformer(qti: Node) = new SelectPointInteractionTransformer(qti)

    def output(qtiNode: Node = qti()) = new RuleTransformer(transformer(qtiNode)).transform(qtiNode)
    def jsonOutput(qtiNode: Node = qti(), identifier: String = identifier): JsObject = {
      transformer(qtiNode).interactionJs(qtiNode).get(identifier)
        .getOrElse(throw new IllegalStateException(s"Missing JSON for $identifier"))
    }

    "transform" should {

      "return <corespring-line/> node" in {
        (output() \\ "corespring-line").head must be equalTo(<corespring-line id={identifier} />)
      }

      "return <p class='prompt'/> containing prompt" in {
        (output() \\ "p").find(p => (p \ "@class").text.contains("prompt"))
          .getOrElse(throw new IllegalStateException("Contained no prompt!")).text must be equalTo(prompt)
      }

      "not return <object/>" in {
        (output() \\ "object").isEmpty must beTrue
      }

    }

    "interactionJS" should {

      "contain componentType = corespring-line" in {
        (jsonOutput() \ "componentType").as[String] must be equalTo("corespring-line")
      }

      "contain domainLabel" in {
        (jsonOutput() \ "model" \ "config" \ "domainLabel").as[String] must be equalTo(getSetting("xAxisTitle"))
      }

      "contain rangeLabel" in {
        (jsonOutput() \ "model" \ "config" \ "rangeLabel").as[String] must be equalTo(getSetting("yAxisTitle"))
      }

      "contain graphWidth" in {
        (jsonOutput() \ "model" \ "config" \ "graphWidth").as[String] must be equalTo(getSetting("gridWidthInPixels"))
      }

      "contain graphHeight" in {
        (jsonOutput() \ "model" \ "config" \ "graphHeight").as[String] must be equalTo(getSetting("gridHeightInPixels"))
      }

      "contain domain" in {
        (jsonOutput() \ "model" \ "config" \ "domain").as[Int] must be equalTo(getSetting("xAxisMaxValue").toInt)
      }

      "contain range" in {
        (jsonOutput() \ "model" \ "config" \ "range").as[Int] must be equalTo(getSetting("yAxisMaxValue").toInt)
      }

      "contain correctResponse in y=mx+b format" in {
        (jsonOutput() \ "correctResponse").as[String] must be equalTo(s"y=${m}x+$b")
      }

    }

  }


}
