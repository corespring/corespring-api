package org.corespring.qtiToV2.interactions

import scala.xml._

object OrderInteractionTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node) = (qti \\ "orderInteraction").map(implicit node => {
    val responses = (responseDeclaration(node, qti) \ "correctResponse" \\ "value").map(_.text)
    val identifier = (node \ "@responseIdentifier").text

    identifier -> Json.obj(
      "componentType" -> "corespring-ordering",
      "correctResponse" -> responses,
      "model" -> Json.obj(
        "prompt" -> ((node \ "prompt") match {
          case seq: Seq[Node] if seq.isEmpty => ""
          case seq: Seq[Node] => seq.head.child.mkString
        }),
        "config" -> Json.obj(
          "shuffle" -> JsBoolean((node \\ "@shuffle").text == "true")),
        "choices" -> (node \\ "simpleChoice")
          .map(choice =>
            Json.obj(
              "label" -> choice.child.filter(_.label != "feedbackInline").mkString.trim,
              "value" -> (choice \ "@identifier").text))),
      "feedback" -> feedback(node, qti))
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "orderInteraction" => {
      val identifier = (e \ "@responseIdentifier").text
      <corespring-ordering id={ identifier }></corespring-ordering>
    }
    case _ => node
  }

}
