package org.corespring.qtiToV2.interactions

import scala.xml._

object ChoiceInteractionTransformer extends InteractionTransformer {

  override def transform(node: Node): Seq[Node] = {
    val identifier = (node \ "@responseIdentifier").text
    node match {
      case elem: Elem if elem.label == "choiceInteraction" => <corespring-multiple-choice id={ identifier }></corespring-multiple-choice>
      case elem: Elem if elem.label == "inlineChoiceInteraction" => <corespring-inline-choice id={ identifier }></corespring-inline-choice>
      case _ => node
    }
  }

  override def interactionJs(qti: Node) = ((qti \\ "choiceInteraction") ++ (qti \\ "inlineChoiceInteraction"))
    .map(implicit node => {

      val componentId = (node \ "@responseIdentifier").text.trim

      def correctResponse: JsObject = {
        val values: Seq[Node] = (responseDeclaration(node, qti) \\ "value").toSeq
        val jsonValues: Seq[JsString] = values.map(n => JsString(n.text.trim))

        Json.obj("value" -> JsArray(jsonValues))
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
            "choiceStyle" -> JsString((node \ "@choiceStyle").text),
            "singleChoice" -> JsBoolean(((node \ "@maxChoices").text == "1"))),
          "prompt" -> (node \ "prompt").map(clearNamespace).text.trim,
          "choices" -> JsArray(((node \\ "simpleChoice").toSeq ++ (node \\ "inlineChoice")).map { n =>
            Json.obj(
              "label" -> n.child.filterNot(e => e.label == "feedbackInline").mkString.trim,
              "value" -> (n \ "@identifier").text.trim)
          })),
        "feedback" -> feedback(node, qti),
        "correctResponse" -> correctResponse)

      componentId -> json

    }).toMap

}
