package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.transform.{RuleTransformer, RewriteRule}
import scala.xml._
import play.api.libs.json._
import play.api.libs.json.JsObject
import scala.Some
import scala.collection.mutable
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import scala.Some

class DragAndDropInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node)
  extends RewriteRule
  with InteractionTransformer {

  var dragAndDropNodes = mutable.Seq[Node]()

  object AnswerAreaTransformer extends RewriteRule {

    private def landingPlace(elem: Elem): Node = {
      elem.copy(label = "span",
        attributes = (new UnprefixedAttribute("landing-place", "landing-place", elem.attributes.toSeq.head)
          ++ elem.attributes).fold(Null)((soFar, attr) => {
          attr.key match {
            case "identifier" => soFar append new UnprefixedAttribute("id", attr.value, soFar.tail.last)
            case _ => soFar append attr
          }
        }))
    }

    override def transform(node: Node): Seq[Node] = node match {
      case e: Elem if e.label == "landingPlace" => landingPlace(e)
      case _ => node
    }

  }

  def component(node: Node) = {
    dragAndDropNodes = dragAndDropNodes :+ node

    val correctResponses = (responseDeclaration(node, qti) \ "correctResponse" \ "value").map(valueNode => {
      ((valueNode \ "@identifier").text -> Json.arr((valueNode \ "value").text))
    })

    val choices = JsArray((node \\ "draggableChoice").map(n =>
      Json.obj(
        "id" -> (n \ "@identifier").text,
        "content" -> n.child.map(clearNamespace).mkString
      )
    ))

    Json.obj(
      "componentType" -> "corespring-drag-and-drop",
      "correctResponse" -> JsObject(correctResponses),
      "model" -> Json.obj(
        "choices" -> choices,
        "prompt" -> ((node \ "prompt") match {
          case seq: Seq[Node] if seq.isEmpty => ""
          case seq: Seq[Node] => seq.head.child.map(clearNamespace).mkString
        }),
        "answerArea" ->
          new RuleTransformer(AnswerAreaTransformer).transform((node \ "answerArea").head)
            .head.child.map(clearNamespace).mkString,
        "config" -> Json.obj(
          "shuffle" -> true,
          "expandHorizontal" -> false
        )
      ),
      "feedback" -> feedback(node, qti)
    )
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
