package org.corespring.poc.integration.impl.transformers.qti.interactions

import org.specs2.mutable.Specification
import scala.xml.{XML, Node}
import scala.collection.mutable
import play.api.libs.json.{Json, JsObject}
import scala.xml.transform.RuleTransformer
import org.corespring.v2player.integration.transformers.qti.interactions.OrderInteractionTransformer

class OrderInteractionTransformerTest extends Specification {

  val identifier = "Q_01"
  val shuffle = "true"
  val prompt = "This is my prompt!"

  def qti(correctResponse: Seq[String]): Node =
    <assessmentItem>
      <responseDeclaration identifier={identifier} cardinality="ordered" baseType="identifier">
        <correctResponse>{correctResponse.map(r => <value>{r}</value>)}</correctResponse>
      </responseDeclaration>
      <itemBody>
        <orderInteraction responseIdentifier={identifier} shuffle={shuffle}>
          <prompt>{prompt}</prompt>
          {correctResponse.map(r => <simpleChoice identifier={r} fixed="true">{r}</simpleChoice>)}
        </orderInteraction>
      </itemBody>
    </assessmentItem>


  "OrderInteractionTransformer" should {

    val responses = List("a", "b", "c")

    val input = qti(responses)
    val componentsJson : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()
    val output = new RuleTransformer(new OrderInteractionTransformer(componentsJson, input)).transform(input)

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
      ((interactionResult \ "correctResponses").as[Seq[String]] zip responses).map{ case (a, b) => a must be equalTo b }
    }

    "return the choices" in {
      val choices = (interactionResult \ "model" \ "choices").as[Seq[JsObject]]
      choices.map(_ \ "label").map(_.as[String]) diff responses must beEmpty
      choices.map(_ \ "value").map(_.as[String]) diff responses must beEmpty
    }

  }

}
