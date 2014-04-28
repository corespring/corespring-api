package org.corespring.v2player.integration.transformers.qti.interactions

import org.specs2.mutable.Specification
import scala.xml.{XML, Node}
import scala.collection.mutable
import play.api.libs.json.{JsUndefined, JsArray, Json, JsObject}
import scala.xml.transform.RuleTransformer

class DragAndDropInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val feedbackValue = "Feedback!"

  def qti(responses: Map[String, String], correctResponses: Map[String, String], shuffle: Option[Boolean] = None,
          itemsPerRow: Option[Int] = None, choicesFirst: Boolean = true): Node =
    <assessmentItem>
      <responseDeclaration identifier={identifier} cardinality="targeted">
        <correctResponse>
          {
            correctResponses.map { case (key, value) => {
              <value identifier={key}>
                <value>{value}</value>
              </value>
            } }
          }
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <dragAndDropInteraction responseIdentifier={identifier}>
          {
            val choices =
                {
                  val choices = responses.map {
                    case (key, value) =>
                      <draggableChoice identifier={key}>{XML.loadString(value)}
                        <feedbackInline identifier={key}>{feedbackValue}</feedbackInline>
                      </draggableChoice>
                  }
                  (shuffle, itemsPerRow) match {
                    case (Some(shuffleValue), Some(itemsPerRowValue)) =>
                      <draggableChoiceGroup shuffle={shuffleValue.toString} itemsPerRow={itemsPerRowValue.toString}>
                        {choices}
                      </draggableChoiceGroup>
                    case (Some(shuffleValue), None) =>
                      <draggableChoiceGroup shuffle={shuffleValue.toString}>{choices}</draggableChoiceGroup>
                    case (None, Some(itemsPerRowValue)) =>
                      <draggableChoiceGroup itemsPerRow={itemsPerRowValue.toString}>{choices}</draggableChoiceGroup>
                    case _ => choices
                  }
                }
             val answers =
              <answerArea>
                {
                correctResponses.keys.map(id => {
                    <landingPlace identifier={id} cardinality="single" />
                })
                }
              </answerArea>
            choicesFirst match {
              case true => Seq(choices, answers)
              case _ => Seq(answers, choices)
            }
          }
        </dragAndDropInteraction>
      </itemBody>
    </assessmentItem>

  "DragAndDropInteractionTransformer" should {

    val correctResponses = Map("a" -> "1", "b" -> "2", "c" -> "3")
    val responses = Map(
      "1" -> "<img src='one.png'/>",
      "2" -> "<img src='two.png'/>",
      "3" -> "<img src='three.png'/>")

    val input = qti(responses, correctResponses)
    val componentsJson = DragAndDropInteractionTransformer.interactionJs(input)
    val output = new RuleTransformer(DragAndDropInteractionTransformer).transform(input)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

    val model = (interactionResult \ "model")
    val config = (model \ "config")

    "return the correct component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-drag-and-drop"
    }

    "return the correct answers for the interaction" in {
      val answers = (interactionResult \ "correctResponse").as[Map[String, Seq[String]]].map{ case (k, v) => (k -> v.head)}.toMap

      answers.map { case (identifier, response) => {
        correctResponses.get(identifier) must not beEmpty ;
        correctResponses.get(identifier).get must be equalTo response
      }}
      answers.keys.toSeq diff correctResponses.keys.toSeq must beEmpty
    }

    "returns default shuffle when none present" in {
      (config \ "shuffle").as[Boolean] must be equalTo DragAndDropInteractionTransformer.Defaults.shuffle
    }

    "returns default expandHorizontal" in {
      (config \ "expandHorizontal").as[Boolean] must be equalTo
        DragAndDropInteractionTransformer.Defaults.expandHorizontal
    }

    "returns no itemsPerRow by default" in {
      (config \ "itemsPerRow") must haveClass[JsUndefined]
    }

    "returns shuffle value when present" in {
      Seq(true, false).map(shuffleValue => {
        val input = qti(responses, correctResponses, shuffle = Some(shuffleValue))
        val interactionResult = DragAndDropInteractionTransformer.interactionJs(input).get(identifier)
          .getOrElse(throw new RuntimeException(s"No component called $identifier"))
        (interactionResult \ "model" \ "config" \ "shuffle").as[Boolean] must be equalTo shuffleValue
      })
    }

    "returns itemsPerRow value when present" in {
      Seq(1, 2, 3, 4).map(itemsPerRowValue => {
        val input = qti(responses, correctResponses, itemsPerRow = Some(itemsPerRowValue))
        val interactionResult = DragAndDropInteractionTransformer.interactionJs(input).get(identifier)
          .getOrElse(throw new RuntimeException(s"No component called $identifier"))
        (interactionResult \ "model" \ "config" \ "itemsPerRow").as[Int] must be equalTo itemsPerRowValue
      })
    }

    "returns choicesPosition 'above' when choices are first" in {
      (config \ "choicesPosition").as[String] must be equalTo "above"
    }

    "returns choicesPosition 'below' when landing places are first" in {
      val input = qti(responses, correctResponses, choicesFirst = false)
      val interactionResult = DragAndDropInteractionTransformer.interactionJs(input).get(identifier)
        .getOrElse(throw new RuntimeException(s"No component called $identifier"))
      (interactionResult \ "model" \ "config" \ "choicesPosition").as[String] must be equalTo "below"
    }

    "removes all <dragAndDropInteraction/> elements" in {
      output \\ "dragAndDropInteraction" must beEmpty
    }

    "return feedback" in {
      val feedback = (interactionResult \ "feedback").as[JsArray].value.map(n => {
        ((n \ "value").as[String] -> (n \ "feedback").as[String])
      }).toMap

      feedback.keys.toSeq diff responses.keys.toSeq must beEmpty
      feedback.values.toSet.toSeq must be equalTo Seq(feedbackValue)
    }

  }

}
