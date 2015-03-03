package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification
import play.api.libs.json._

class NumberLineInteractionTransformerTest extends Specification {

  "NumberLineInteractionTransformer" should {

    val identifier = "1"
    val lowerBound = 1
    val upperBound = 2
    val step = 0.125
    val correctResponses = Seq(1.125, 1.25, 1.50)
    val hatchMarks = Map[String, (Double, Boolean)]("1" -> (1d, true), "1.125" -> (1.125, false))

    def qti(identifier: String = identifier,
            lowerBound: Int = lowerBound, upperBound: Int = upperBound, step: Double = step,
            hatchMarks: Map[String, (Double, Boolean)] = hatchMarks,
            correctResponses: Seq[Double] = correctResponses) =
      <assessmentItem>
        <responseDeclaration identifier={identifier}>
          <correctResponse>
            {correctResponses.map(r => <value>{r}</value>)}
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <numberLineInteraction responseIdentifier={identifier} lowerBound={lowerBound.toString}
                                 upperBound={upperBound.toString} step={step.toString} titleAbove="false">
            {hatchMarks.map{ case(label, (value, isVisible)) => <hatchMark value={value.toString} label={label} isVisibleLabel={if (isVisible) "1" else "0"} /> } }
          </numberLineInteraction>
          </itemBody>
        </assessmentItem>

    val result = NumberLineInteractionTransformer.interactionJs(qti()).headOption.getOrElse(throw new Exception("There was an error translating QTI"))._2

    "transform correctResponse" in {
      val responses = (result \ "correctResponse").as[Seq[JsObject]]
      correctResponses.zip(responses).map{ case(expected, response) => {
        (response \ "type").as[String] must be equalTo("point")
        (response \ "pointType").as[String] must be equalTo("full")
        (response \ "domainPosition").as[Double] must be equalTo(expected)
      }}.head
    }

    "transform config" in {
      (result \ "model" \ "config" \ "initialType").as[String] must be equalTo "PF"
      (result \ "model" \ "config" \ "exhibitOnly").as[Boolean] must beFalse
    }

    "transform domain" in {
      (result \ "model" \ "config" \ "domain").as[Seq[Int]] must be equalTo Seq(lowerBound, upperBound)
    }

    "transform tickFrequency" in {
      (result \ "model" \ "config" \ "tickFrequency").as[Double] must be equalTo((upperBound - lowerBound) / step)
    }

    "transform hatchMarks" in {
      (result \ "model" \ "config" \ "ticks").as[Seq[JsObject]]
        .map(tick => (tick \ "value").as[Double] -> (tick \ "label").as[String])
        .toMap must be equalTo hatchMarks.map{ case (k, (v, visible)) => v -> (if (visible) k.toString else "")}.toMap
    }

  }

}
