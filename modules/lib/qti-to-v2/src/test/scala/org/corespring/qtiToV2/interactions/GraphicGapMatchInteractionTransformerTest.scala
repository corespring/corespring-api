package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification
import play.api.libs.json._

import scala.xml.transform.RuleTransformer
import scala.xml.{Elem, Node, NodeSeq, XML}

class GraphicGapMatchInteractionTransformerTest extends Specification {

  def qti(rd: Elem, body: Elem): Node =
    <assessmentItem>
      <correctResponseFeedback>Default Correct</correctResponseFeedback>
      <incorrectResponseFeedback>Default Incorrect</incorrectResponseFeedback>{rd}<itemBody>
      {body}
    </itemBody>
    </assessmentItem>

  def responseDeclaration(correctResponse: Elem) =
    <responseDeclaration identifier="Q_01" cardinality="multiple" baseType="directedPair">
      {correctResponse}
    </responseDeclaration>

  def prompt = "ITEM <b>PROMPT</b>"

  def graphicGapMatchInteraction = XML.loadString( s"""
    <graphicGapMatchInteraction responseIdentifier="Q_01">
      <prompt>$prompt</prompt>
      <object data="../images/ROGJOH370_Rocket_stem_01_o_b288978462.png" height="343" type="image/png" width="379"/>
      <gapImg identifier="GI-6027" matchMax="1">
        <object data="../images/ROGJOH370_Rocket_opt_C01_o_4fad1b2ea3.png" height="49" type="image/png" width="175"/>
      </gapImg>
      <gapImg identifier="GI-6026" matchMax="1">
        <object data="../images/ROGJOH370_Rocket_opt_B01_o_e77e66f7dd.png" height="49" type="image/png" width="175"/>
      </gapImg>
      <gapImg identifier="GI-6028" matchMax="1">
        <object data="../images/ROGJOH370_Rocket_opt_D01_o_11532888df.png" height="49" type="image/png" width="174"/>
      </gapImg>
      <gapImg identifier="GI-6029" matchMax="1">
        <object data="../images/ROGJOH370_Rocket_opt_E01_o_7d00da2f78.png" height="49" type="image/png" width="174"/>
      </gapImg>
      <gapImg identifier="GI-6025" matchMax="1">
        <object data="../images/ROGJOH370_Rocket_opt_A01_o_d3d709b145.png" height="49" type="image/png" width="175"/>
      </gapImg>
      <gapImg identifier="GI-6030" matchMax="1">
        <object data="../images/ROGJOH370_Rocket_opt_F01_o_ba6d73a6e8.png" height="49" type="image/png" width="174"/>
      </gapImg>
      <associableHotspot coords="197,30,372,79" identifier="HS-6031" matchMax="1" shape="rect"/>
      <associableHotspot coords="198,96,373,145" identifier="HS-6032" matchMax="1" shape="rect"/>
      <associableHotspot coords="197,169,372,218" identifier="HS-6033" matchMax="1" shape="rect"/>
      <associableHotspot coords="196,280,371,329" identifier="HS-6034" matchMax="1" shape="rect"/>
      <associableHotspot coords="155,2,105,11,84,18,60,27" identifier="HS-6035" matchMax="1" shape="poly"/>
    </graphicGapMatchInteraction>
  """)

  val interaction = qti(
    responseDeclaration(<correctResponse>
      <value>GI-6026 HS-6031</value>
      <value>GI-6025 HS-6032</value>
      <value>GI-6027 HS-6033</value>
      <value>GI-6029 HS-6034</value>
    </correctResponse>),
    graphicGapMatchInteraction)

  val interactionWithMappedValues = qti(
    responseDeclaration(<correctResponse>
      <mapping lowerBound="0" upperBound="6" defaultValue="0">
        <mapEntry mapKey="GI-6026 HS-6031" mappedValue="1"/>
        <mapEntry mapKey="GI-6025 HS-6032" mappedValue="1"/>
        <mapEntry mapKey="GI-6027 HS-6033" mappedValue="1"/>
        <mapEntry mapKey="GI-6029 HS-6034" mappedValue="1"/>
      </mapping>
    </correctResponse>),
    graphicGapMatchInteraction)

  "GraphicGapMatchInteractionTransformer" should {

    "transform interaction" in {
      val out = new RuleTransformer(GraphicGapMatchInteractionTransformer).transform(interaction)
      val componentsJson = GraphicGapMatchInteractionTransformer.interactionJs(interaction)
      val q1 = componentsJson.get("Q_01").getOrElse(throw new RuntimeException("No component called Q_01"))

      (out \\ "p").head.child.mkString === prompt
      (q1 \ "componentType").as[String] === "corespring-graphic-gap-match"

      (q1 \ "model" \ "config" \ "backgroundImage").as[JsObject] === Json.obj(
        "path" -> "ROGJOH370_Rocket_stem_01_o_b288978462.png",
        "width" -> 379,
        "height" -> 343
      )

      val choices = (q1 \ "model" \ "choices").as[Seq[JsObject]]
      choices.length === 6
      choices(0) === Json.obj("id" -> "GI-6027",
        "label" -> "<img src='ROGJOH370_Rocket_opt_C01_o_4fad1b2ea3.png' width='175' height='49' />",
        "matchMax" -> 1,
        "matchMin" -> 0)

      choices(5) === Json.obj("id" -> "GI-6030",
        "label" -> "<img src='ROGJOH370_Rocket_opt_F01_o_ba6d73a6e8.png' width='174' height='49' />",
        "matchMax" -> 1,
        "matchMin" -> 0)

      val hotspots = (q1 \ "model" \ "hotspots").as[Seq[JsObject]]
      ((q1 \ "model" \ "hotspots") \\ "id").map(_.as[String]) === Seq("HS-6031", "HS-6032", "HS-6033", "HS-6034", "HS-6035")
      hotspots(0) === Json.obj("id" -> "HS-6031",
        "shape" -> "rect",
        "coords" -> Json.obj(
          "left" -> 197.0,
          "top" -> 30.0,
          "width" -> 175.0,
          "height" -> 49.0
        )
      )

      hotspots(4) === Json.obj("id" -> "HS-6035",
        "shape" -> "poly",
        "coords" -> JsArray(Seq(
          Json.obj("x" -> 155.0, "y" -> 2.0),
          Json.obj("x" -> 105.0, "y" -> 11.0),
          Json.obj("x" -> 84.0, "y" -> 18.0),
          Json.obj("x" -> 60.0, "y" -> 27.0)
        )
        )
      )

      val correctResponse = (q1 \ "correctResponse").as[Seq[JsObject]]
      correctResponse === Seq(Json.obj("id" -> "GI-6026", "hotspot" -> "HS-6031"),
        Json.obj("id" -> "GI-6025", "hotspot" -> "HS-6032"),
        Json.obj("id" -> "GI-6027", "hotspot" -> "HS-6033"),
        Json.obj("id" -> "GI-6029", "hotspot" -> "HS-6034")
      )
    }

    "transform interaction with mapped correct response" in {
      val out = new RuleTransformer(GraphicGapMatchInteractionTransformer).transform(interactionWithMappedValues)
      val componentsJson = GraphicGapMatchInteractionTransformer.interactionJs(interactionWithMappedValues)
      val q1 = componentsJson.get("Q_01").getOrElse(throw new RuntimeException("No component called Q_01"))

      val correctResponse = (q1 \ "correctResponse").as[Seq[JsObject]]
      correctResponse === Seq(Json.obj("id" -> "GI-6026", "hotspot" -> "HS-6031"),
        Json.obj("id" -> "GI-6025", "hotspot" -> "HS-6032"),
        Json.obj("id" -> "GI-6027", "hotspot" -> "HS-6033"),
        Json.obj("id" -> "GI-6029", "hotspot" -> "HS-6034")
      )

    }
  }
}
