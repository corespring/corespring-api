package org.corespring.v2player.integration.transformers.qti.interactions

import org.corespring.qtiToV2.interactions.OrderInteractionTransformer
import org.specs2.mutable.Specification
import scala.xml.Node
import scala.collection.mutable
import play.api.libs.json.{ JsArray, JsObject }
import scala.xml.transform.RuleTransformer
import org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4

class OrderInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val shuffle = "true"
  val prompt = "This is my prompt!"
  val feedbackValue = "Feedback!"

  def qti(correctResponse: Seq[String]): Node =
    <assessmentItem>
      <responseDeclaration identifier={ identifier } cardinality="ordered" baseType="identifier">
        <correctResponse>{ correctResponse.map(r => <value>{ r }</value>) }</correctResponse>
      </responseDeclaration>
      <itemBody>
        <orderInteraction responseIdentifier={ identifier } shuffle={ shuffle }>
          <prompt>{ prompt }</prompt>
          {
            correctResponse.map(r =>
              <simpleChoice identifier={ r } fixed="true">
                { r }
                <feedbackInline identifier={ r }>{ feedbackValue }</feedbackInline>
              </simpleChoice>)
          }
        </orderInteraction>
      </itemBody>
    </assessmentItem>

  "OrderInteractionTransformer" should {

    val responses = List("a", <img src="puppies.png"/>.toString, "c")

    val input = qti(responses)
    val componentsJson = OrderInteractionTransformer.interactionJs(input)
    val output = new RuleTransformer(OrderInteractionTransformer).transform(input)

    val interactionResult =
      componentsJson.get(identifier).getOrElse(throw new RuntimeException(s"No component called $identifier"))

    "result must contain <corespring-ordering/>" in {
      (output \\ "corespring-ordering").find(n => (n \ "@id").text == identifier) must not beEmpty
    }

    "must contain appropriate shuffle property" in {
      (interactionResult \ "model" \ "config" \ "shuffle").as[Boolean] must be equalTo shuffle.toBoolean
    }

    "must contain the appropriate prompt" in {
      (interactionResult \ "model" \ "prompt").as[String] must be equalTo prompt
    }

    "return the correct component type" in {
      (interactionResult \ "componentType").as[String] must be equalTo "corespring-ordering"
    }

    "return the correct response" in {
      ((interactionResult \ "correctResponse").as[Seq[String]] zip responses).map { case (a, b) => a must be equalTo b }
    }

    "return the choices" in {
      val choices = (interactionResult \ "model" \ "choices").as[Seq[JsObject]]
      println(choices)
      choices.map(_ \ "label").map(_.as[String]).map(unescapeHtml4) diff responses must beEmpty
      choices.map(_ \ "value").map(_.as[String]).map(unescapeHtml4) diff responses must beEmpty
    }

    "return feedback" in {
      val feedback = (interactionResult \ "feedback").as[JsArray].value.map(n => {
        ((n \ "value").as[String] -> (n \ "feedback").as[String])
      }).toMap

      feedback.keys.toSeq diff responses.toSeq must beEmpty
      feedback.values.toSet.toSeq must be equalTo Seq(feedbackValue)
    }

  }

}
