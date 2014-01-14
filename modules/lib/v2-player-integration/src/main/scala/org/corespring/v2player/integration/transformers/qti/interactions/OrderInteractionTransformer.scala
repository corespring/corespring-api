package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.transform.RewriteRule
import scala.xml.{Text, Elem, Node}
import scala.collection.mutable
import play.api.libs.json.{JsBoolean, Json, JsObject}

class OrderInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node)
  extends RewriteRule
  with InteractionTransformer {

  def component(node: Node) = {
    val responses = (responseDeclaration(node, qti) \ "correctResponse" \\ "value").map(_.text)

    Json.obj(
      "componentType" -> "corespring-ordering",
      "correctResponse" -> responses,
      "model" -> Json.obj(
        "prompt" -> ((node \ "prompt") match {
          case seq: Seq[Node] if seq.isEmpty => ""
          case seq: Seq[Node] => seq.head.child.mkString
        }),
        "config" -> Json.obj(
          "shuffle" -> JsBoolean((node \\ "@shuffle").text == "true")
        ),
        "choices" -> (node \\ "simpleChoice")
          .map(choice =>
            Json.obj(
              "label" -> choice.child.filter(_.label != "feedbackInline").text.trim,
              "value" -> (choice \ "@identifier").text))
      ),
      "feedback" -> feedback(node, qti)
    )
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
