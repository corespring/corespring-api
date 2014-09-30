package org.corespring.qtiToV2.interactions

import play.api.libs.json._

import scala.xml._

object ChoiceInteractionTransformer extends InteractionTransformer {

  override def transform(node: Node): Seq[Node] = {
    val identifier = (node \ "@responseIdentifier").text
    node match {
      case elem: Elem if elem.label == "choiceInteraction" => <corespring-multiple-choice id={ identifier }></corespring-multiple-choice>.withPrompt(node)
      case elem: Elem if elem.label == "inlineChoiceInteraction" => <corespring-inline-choice id={ identifier }></corespring-inline-choice>.withPrompt(node)
      case _ => node
    }
  }

  override def interactionJs(qti: Node) = ((qti \\ "choiceInteraction") ++ (qti \\ "inlineChoiceInteraction"))
    .map(implicit node => {

      val componentId = (node \ "@responseIdentifier").text.trim

      def correctResponses: Seq[JsString] = {
        val values: Seq[Node] = (responseDeclaration(node, qti) \\ "value").toSeq
        values.map(n => JsString(n.text.trim))
      }

      def choiceStyle = {
        ((node \ "@choiceStyle").text, correctResponses.length) match {
          case (choiceStyle, responseCount) if (choiceStyle.isEmpty) => if (responseCount == 1) "radio" else "checkbox"
          case (choiceStyle, responseCount) => choiceStyle
        }
      }

      val json = Json.obj(
        "componentType" -> (node.label match {
          case "choiceInteraction" => "corespring-multiple-choice"
          case "inlineChoiceInteraction" => "corespring-inline-choice"
          case _ => throw new IllegalStateException
        }),
        "model" -> Json.obj(
          "config" -> Json.obj(
            "shuffle" -> (node \ "@shuffle").text,
            "orientation" -> JsString(if ((node \ "@orientation").text == "horizontal") "horizontal" else "vertical"),
            "choiceStyle" -> JsString(choiceStyle),
            "singleChoice" -> JsBoolean(((node \ "@maxChoices").text == "1"))),
          "prompt" -> (node \ "prompt").map(clearNamespace).text.trim,
          "choices" -> JsArray(((node \\ "simpleChoice").toSeq ++ (node \\ "inlineChoice")).map { n =>
            Json.obj(
              "label" -> n.child.filterNot(e => e.label == "feedbackInline").mkString.trim,
              "value" -> (n \ "@identifier").text.trim)
          })),
        "feedback" -> feedback(node, qti),
        "correctResponse" -> (node.label match {
          case "choiceInteraction" => Json.obj("value" -> JsArray(correctResponses))
          case "inlineChoiceInteraction" => correctResponses(0)
          case _ => throw new IllegalStateException
        }))

      componentId -> json

    }).toMap

}
