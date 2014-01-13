package org.corespring.v2player.integration.transformers.qti.interactions

import org.specs2.mutable.Specification
import scala.collection.mutable
import play.api.libs.json.JsObject
import scala.xml.transform.RuleTransformer

class PointInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val correctResponses = Seq("0,1", "1,0")
  val pointLabels = Seq("A", "B")
  val maxPoints = 2

  def qti(correctResponses: Seq[String]) =
    <assessmentItem>
      <responseDeclaration identifier={identifier}>
        <correctResponse>
          {
            correctResponses.map(response => <value>{response}</value>)
          }
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <pointInteraction responseIdentifier={identifier} point-labels={pointLabels.mkString(",")}
                          max-points={maxPoints.toString} />
      </itemBody>
    </assessmentItem>

  "PointInteractionTransformer" should {

    val input = qti(correctResponses)
    val componentsJson : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()
    val output = new RuleTransformer(new PointInteractionTransformer(componentsJson, input)).transform(input)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

    val config = (interactionResult \ "model" \ "config")

    "return the correct component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-point-intercept"
    }

    "returns correct response" in {
      (interactionResult \ "correctResponse").as[Seq[String]] diff correctResponses must beEmpty
    }

    "returns correct point labels" in {
      (config \ "pointLabels").as[String] must be equalTo pointLabels.mkString(",")
    }

    "returns correct max points" in {
      (config \ "maxPoints").as[Int] must be equalTo maxPoints
    }

    "removes all <pointInteraction/> elements" in {
      output \\ "pointInteraction" must beEmpty
    }

  }

}
