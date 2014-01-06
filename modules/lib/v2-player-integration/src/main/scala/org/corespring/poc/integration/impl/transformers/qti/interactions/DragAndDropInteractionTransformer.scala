package org.corespring.poc.integration.impl.transformers.qti.interactions

import scala.xml.transform.{RuleTransformer, RewriteRule}
import scala.xml._
import play.api.libs.json._
import play.api.libs.json.JsObject
import scala.Some
import scala.collection.mutable

class DragAndDropInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node) extends RewriteRule {

  var dragAndDropNodes = mutable.Seq[Node]()

  object AnswerAreaTransformer extends RewriteRule {

    def landingPlace(node: Node): Node = {
      val identifier = (node \ "@identiifer").text
      <span landing-place="landing-place" style="display: inline;" identifier={identifier} />
    }

    override def transform(node: Node): Seq[Node] = node match {
      case e: Elem if e.label == "landingPlace" => landingPlace(e)
      case _ => node
    }

  }

  def component(node: Node) = {
    dragAndDropNodes = dragAndDropNodes :+ node

    val identifier = (node \\ "@responseIdentifier").text
    (qti \\ "responseDeclaration").find(n => (n \ "@identifier").text == identifier) match {
      case Some(responseDeclaration) => {

        val correctResponses = (responseDeclaration \ "correctResponse" \ "value").map(valueNode => {
          ((valueNode \ "@identifier").text -> Json.arr((valueNode \ "value").text))
        })

        val choices = JsArray((node \\ "draggableChoice").map(n =>
          Json.obj(
            "id" -> (n \ "@identifier").text,
            "content" -> n.child.mkString
          )
        ))

        Json.obj(
          "componentType" -> "corespring-drag-and-drop",
          "correctResponse" -> JsObject(correctResponses),
          "model" -> Json.obj(
            "choices" -> choices,
            "prompt" -> (node \ "prompt").head.child.mkString,
            "answerArea" -> new RuleTransformer(AnswerAreaTransformer).transform((node \ "answerArea").head).head.child.mkString,
            "config" -> Json.obj(
              "shuffle" -> true,
              "expandHorizontal" -> false
            )
          )
        )
      }
      case None =>
        throw new IllegalStateException(s"Item did not contain a responseDeclaration for interaction $identifier")
    }
  }

  (qti \\ "dragAndDropInteraction").foreach(node => {
    componentJson.put((node \\ "@responseIdentifier").text, component(node))
  })

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "dragAndDropInteraction" => {
      val identifier = (e \ "@responseIdentifier").text
      <corespring-drag-and-drop id={identifier} />
    }
    case _ => node
  }

}
