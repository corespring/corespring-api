package org.corespring.poc.integration.impl.transformers.qti.interactions

import scala.xml.transform.RewriteRule
import play.api.Logger
import scala.xml.{Elem, Node}
import scala.collection.mutable
import play.api.libs.json.{Json, JsObject}
import org.corespring.qti.models.QtiItem

class TextEntryInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node) extends RewriteRule {

  private val logger: Logger = Logger("poc.integration")

  private def feedback(n: Node) = {
    JsObject(
      (n \\ "feedbackBlock").map(feedbackBlock => {
        (feedbackBlock \ "@identifier").text -> Json.obj(
          "feedback" -> feedbackBlock.child.toString,
          "correct" -> true
        )
      })
    )
  }


  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "textEntryInteraction" => {
      val qtiItem = QtiItem(qti)
      val id = e \\ "@identifier"
      val responseIdentifier = (e \ "@responseIdentifier").text

      componentJson.put(responseIdentifier, Json.obj(
        "componentType" -> "corespring-text-entry",
        "feedback" -> feedback(qti)
      ))
      <corespring-text-entry id={responseIdentifier}></corespring-text-entry>
    }
    case _ => node
  }

}
