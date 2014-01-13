package org.corespring.poc.integration.impl.transformers.qti.interactions

import org.specs2.mutable.Specification
import scala.xml.{XML, Node}
import scala.collection.mutable
import play.api.libs.json.JsObject
import scala.xml.transform.RuleTransformer
import org.corespring.v2player.integration.transformers.qti.interactions.DragAndDropInteractionTransformer


class DragAndDropInteractionTransformerTest extends Specification {

  val identifier = "Q_01"

  def qti(responses: Map[String, String], correctResponses: Map[String, String]): Node =
    <assessmentItem>
      <responseDeclaration identifier={identifier}>
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
            responses.map { case (key, value) => {
              <draggableChoice identifier={key}>{XML.loadString(value)}</draggableChoice>
            } }
          }
          <answerArea>
            {
              responses.keys.map(id => {
                <landingPlace identifier={id} />
              })
            }
          </answerArea>
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
    val componentsJson : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()
    val output = new RuleTransformer(new DragAndDropInteractionTransformer(componentsJson, input)).transform(input)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

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

    "removes all <dragAndDropInteraction/> elements" in {
      output \\ "dragAndDropInteraction" must beEmpty
    }

  }

}
