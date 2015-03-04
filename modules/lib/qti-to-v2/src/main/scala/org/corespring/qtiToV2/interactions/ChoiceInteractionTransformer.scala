package org.corespring.qtiToV2.interactions

import play.api.libs.json._

import scala.xml._

object ChoiceInteractionTransformer extends InteractionTransformer {

  override def transform(node: Node): Seq[Node] = {
    val identifier = (node \ "@responseIdentifier").text
    node match {
      case elem: Elem if elem.label == "choiceInteraction" =>
        elem.child.filter(_.label != "simpleChoice").map(n => n.label match {
          case "prompt" => <p class="prompt">{n.child}</p>
          case _ => n
        }) ++ <corespring-multiple-choice id={ identifier }></corespring-multiple-choice>
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

      def choiceType(node: Node): String = {
        correctResponses.length match {
          case length: Int if length > 1 => "checkbox"
          case _ => if ((node \\ "@maxChoices").text == "1") "radio" else "checkbox"
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
            "choiceType" -> JsString(choiceType(node)),
            "choiceLabels" -> JsString("letters"),
            "choiceStyle" -> JsString((node \ "@choiceStyle").text),
            "choiceType" -> JsString(if ((node \ "@maxChoices").text == "1") "radio" else "checkbox"),
            "showCorrectAnswer" -> JsString(if (correctResponses.length == 1) "inline" else "separately")
          ),
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
