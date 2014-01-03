package org.corespring.poc.integration.impl.transformers.qti.interactions

import scala.xml.transform.RewriteRule
import scala.xml.{Elem, Node}
import scala.collection.mutable
import play.api.libs.json.{Json, JsObject}

class TextEntryInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node) extends RewriteRule {

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

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "textEntryInteraction" => {
      val responseIdentifier = (e \ "@responseIdentifier").text
      componentJson.put(responseIdentifier, component(qti, responseIdentifier))
      <corespring-text-entry id={responseIdentifier}></corespring-text-entry>
    }
    case _ => node
  }

}
