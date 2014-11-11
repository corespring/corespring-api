package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification
import play.api.libs.json._

class HottextInteractionTransformerTest extends Specification {

  "HottextInteractionTransformer" should {

    val responseIdentifier = "1"
    val choices = Map("1" -> "one", "2" -> "two", "3" -> "three")
    val correctChoices = Seq("1")

    def qti(responseIdentifier: String = responseIdentifier,
            choices: Map[String, String] = choices,
            correctChoices: Seq[String] = correctChoices) =
      <assessmentItem>
        <responseDeclaration identifier={responseIdentifier} cardinality="single" baseType="identifier">
          <correctResponse>{ correctChoices.map(choice => <value>{choice}</value>) }</correctResponse>
        </responseDeclaration>
        <itemBody>
          <hottextInteraction responseIdentifier={responseIdentifier} maxChoices="1">{
              choices.map{case (identifier, text) => {
                <hottext identifier={identifier}>{text}</hottext>
              }}
          }</hottextInteraction>
        </itemBody>
      </assessmentItem>

    val result = HottextInteractionTransformer.interactionJs(qti()).get(responseIdentifier)
      .getOrElse(throw new IllegalStateException(s"Missing result for $responseIdentifier"))

    "transform choices" in {
      (result \ "model" \ "choices")
        .as[Seq[JsObject]].map(c => (c \ "data").as[String]) must be equalTo choices.values.toSeq
    }

    "transform correctness" in {
      (result \ "model" \ "choices").as[Seq[JsObject]].filter(c => (c \ "correct").asOpt[Boolean].getOrElse(false))
        .map(c => (c \ "data").as[String]).map(choices.map(_.swap).get(_)).flatten must be equalTo correctChoices
    }

  }

}
