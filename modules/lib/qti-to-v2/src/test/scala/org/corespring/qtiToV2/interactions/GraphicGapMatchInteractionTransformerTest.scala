package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification
import play.api.libs.json._

import scala.xml.transform.RuleTransformer
import scala.xml.{Elem, Node, NodeSeq, XML}

class GraphicGapMatchInteractionTransformerTest extends Specification {

  def qti(rd: Elem, body: Elem): Node =
    <assessmentItem>
      <correctResponseFeedback>Default Correct</correctResponseFeedback>
      <incorrectResponseFeedback>Default Incorrect</incorrectResponseFeedback>
      { rd }<itemBody>
              { body }
            </itemBody>
    </assessmentItem>

  def responseDeclaration(correctResponse: Elem) =
    <responseDeclaration identifier="Q_01" cardinality="multiple" baseType="directedPair">
      { correctResponse }
    </responseDeclaration>

  def prompt = "ITEM <b>PROMPT</b>"

  def graphicGapMatchInteraction = XML.loadString(s"""
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

  "GraphicGapMatchInteractionTransformer" should {

    "transform interaction" in {
      val out = new RuleTransformer(GraphicGapMatchInteractionTransformer).transform(interaction)
      val componentsJson = GraphicGapMatchInteractionTransformer.interactionJs(interaction)

      val q1 = componentsJson.get("Q_01").getOrElse(throw new RuntimeException("No component called Q_01"))
      println(q1 \ "model" \ "hotspots")

      (out \\ "p").head.child.mkString === prompt
      (q1 \ "componentType").as[String] === "corespring-graphic-gap-match"
      (q1 \ "model" \ "config" \ "backgroundImage").as[String] === "ROGJOH370_Rocket_stem_01_o_b288978462.png"
      val choices = (q1 \ "model" \ "choices").as[Seq[JsObject]]
      choices.length === 6
      (choices(0) \ "label").as[String] === "<img src='ROGJOH370_Rocket_opt_C01_o_4fad1b2ea3.png' />"
//      ((q1 \ "model" \ "choices")(0) \ "label").as[String] === a.toString
//      (q1 \ "correctResponse" \ "value") === JsArray(Seq(JsString("A")))
//      (q1 \ "feedback").as[Seq[JsObject]].length === 2
//      ((q1 \ "feedback")(0) \ "value").as[String] === "A"
//      ((q1 \ "feedback")(0) \ "feedback").as[String] === "Default Correct"
    }
  }
}
