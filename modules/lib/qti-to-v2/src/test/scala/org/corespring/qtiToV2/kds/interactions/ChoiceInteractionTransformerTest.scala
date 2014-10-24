package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification
import play.api.libs.json._

class ChoiceInteractionTransformerTest extends Specification {

  "ChoiceInteractionTransformer" should {

    val responseIdentifier = "1"
    val correctResponse = "2"
    val choices = Map("1" -> "choice one", correctResponse -> "choice two")
    val rationales = Map("1" -> "This is why choice one", correctResponse -> "This is why choice two")
    val shuffle = false
    val maxChoices = "1"

    def qti(responseIdentifier: String = responseIdentifier,
                              correctResponse: String = correctResponse,
                              choices: Map[String, String] = choices,
                              shuffle: Boolean = shuffle,
                              maxChoices: String = maxChoices) =
      <assessmentItem>
        <responseDeclaration identifier={responseIdentifier} cardinality="single">
          <correctResponse>
            <value>{correctResponse}
            </value>
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <choiceInteraction responseIdentifier={responseIdentifier} shuffle={shuffle.toString} maxChoices={maxChoices}>
            {choices.map { case (id, text) => <simpleChoice identifier={id}>{text}</simpleChoice>}}
          </choiceInteraction>
          <choiceRationales responseIdentifier={responseIdentifier}>
            {rationales.map { case (id, rationale) => <rationale identifier={id}>{rationale}</rationale>}}
          </choiceRationales>
        </itemBody>
      </assessmentItem>

    val result = ChoiceInteractionTransformer.interactionJs(qti())

    "transform rationales" in {
      val json = result.values.headOption.getOrElse(throw new Exception("There was no result"))
      val rationaleResult = (json \ "model" \ "choices").as[Seq[JsObject]].map(f => (f \ "value").as[String] -> (f \ "rationale").as[String]).toMap
      rationaleResult must be equalTo(rationales)
    }

  }

}
