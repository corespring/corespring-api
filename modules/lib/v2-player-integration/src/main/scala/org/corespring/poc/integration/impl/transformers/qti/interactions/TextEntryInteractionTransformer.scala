package org.corespring.poc.integration.impl.transformers.qti.interactions

import scala.xml.transform.RewriteRule
import play.api.Logger
import scala.xml.{Elem, Node}
import scala.collection.mutable
import play.api.libs.json.{JsString, Json, JsObject}
import org.corespring.qti.models.QtiItem

class TextEntryInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node) extends RewriteRule {

  private val logger: Logger = Logger("poc.integration")

  private def component(n: Node, identifier: String) = {
    val correctResponses = ((n \\ "responseDeclaration")
      .filter(n => (n \ "@identifier").toString == identifier).head \ "correctResponse" \\ "value").map(_.text).toSet
    Json.obj(
      "componentType" -> "corespring-text-entry",
      "correctResponse" -> (correctResponses.size match {
        case 0 => Json.arr()
        case 1 => correctResponses.head
        case _ => correctResponses
      })
    )
  }

  private def feedback(n: Node, identifier: String) = {
    val feedbackBlocks = (n \\ "feedbackBlock")
      .filter(n => (n \ "@outcomeIdentifier").toString == s"responses.$identifier.value")
    Json.obj(
      "componentType" -> "corespring-feedback-block",
      "target" -> Json.obj("id" -> identifier),
      "feedback" -> Json.obj(
        "correct" -> JsObject(
          feedbackBlocks.filter(n => (n \ "@incorrectResponse").toString != "true").map(feedbackBlock => {
            (feedbackBlock \ "@identifier").text match {
              case "" => "*" -> JsString(feedbackBlock.child.text.trim)
              case _ => (feedbackBlock \ "@identifier").text -> JsString(feedbackBlock.child.text.trim)
            }
          })
        ),
        "incorrect" -> JsObject(
          feedbackBlocks.filter(n => (n \ "@incorrectResponse").toString == "true").map(feedbackBlock => {
            (feedbackBlock \ "@identifier").text match {
              case "" => "*" -> JsString(feedbackBlock.child.text.trim)
              case _ => (feedbackBlock \ "@identifier").text -> JsString(feedbackBlock.child.text.trim)
            }
          })
        )
      )
    )
  }

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "textEntryInteraction" => {
      val qtiItem = QtiItem(qti)
      val id = e \\ "@identifier"
      val responseIdentifier = (e \ "@responseIdentifier").text

      componentJson.put(responseIdentifier, component(qti, responseIdentifier))
      componentJson.put(s"${responseIdentifier}_feedback", feedback(qti, responseIdentifier))
      <corespring-text-entry id={responseIdentifier}></corespring-text-entry>
    }
    case _ => node
  }

}
