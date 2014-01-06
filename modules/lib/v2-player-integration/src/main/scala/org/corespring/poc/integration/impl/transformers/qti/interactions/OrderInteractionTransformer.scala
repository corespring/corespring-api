package org.corespring.poc.integration.impl.transformers.qti.interactions

import scala.xml.transform.RewriteRule
import scala.xml.{Elem, Node}
import scala.collection.mutable
import play.api.libs.json.{JsBoolean, Json, JsObject}

class OrderInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node) extends RewriteRule {

  def component(node: Node) = {
    val identifier = (node \\ "@responseIdentifier").text

    (qti \\ "responseDeclaration").find(n => (n \ "@identifier").text == identifier) match {
      case Some(responseDeclaration) => {
        val responses = (responseDeclaration \ "correctResponse" \\ "value").map(_.text)
        Json.obj(
          "componentType" -> "corespring-ordering",
          "correctResponses" -> responses,
          "model" -> Json.obj(
            "prompt" -> ((node \ "prompt") match {
              case seq: Seq[Node] if seq.isEmpty => ""
              case seq: Seq[Node] => seq.head.child.mkString
            }),
            "config" -> Json.obj(
              "shuffle" -> JsBoolean((node \\ "@shuffle").text == "true")
            ),
            "choices" -> (node \\ "simpleChoice")
              .map(choice => Json.obj("label" -> choice.text, "value" -> (choice \\ "@identifier").text))
          )
        )
      }
      case None =>
        throw new IllegalStateException(s"Item did not contain a responseDeclaration for interaction $identifier")
    }

  }

  (qti \\ "orderInteraction").foreach(node => {
    componentJson.put((node \\ "@responseIdentifier").text, component(node))
  })

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "orderInteraction" => {
      val identifier = (e \ "@responseIdentifier").text
      <corespring-ordering id={identifier} />
    }
    case _ => node
  }

}
