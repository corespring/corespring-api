package org.corespring.v2player.integration.transformers.qti.interactions

import play.api.libs.json._
import scala.xml._

object ChoiceInteractionTransformer extends InteractionTransformer {

  override def transform(node: Node): Seq[Node] = {
    val identifier = (node \ "@responseIdentifier").text
    node match {
      case elem: Elem if elem.label == "choiceInteraction" => <corespring-line id={identifier} />
      case elem: Elem if elem.label == "inlineChoiceInteraction" => <corespring-inline-choice id={identifier} />
      case _ => node
    }
  }

  override def interactionJs(qti: Node) = ((qti \\ "choiceInteraction") ++ (qti \\ "inlineChoiceInteraction"))
    .map(implicit node => {

      val componentId = (node \ "@responseIdentifier").text.trim

      def choices: JsArray = {
        val out : Seq[JsValue] = ((node \\ "simpleChoice").toSeq ++ (node \\ "inlineChoice")).map{ n: Node =>
          Json.obj(
            "label" -> n.child.filter(_.isInstanceOf[Text]).mkString,
            "value" -> (n \ "@identifier").text.trim
          )
        }
        JsArray(out)
      }

      def correctResponse: JsObject = {
        val values: Seq[Node] = (responseDeclaration(node, qti) \\ "value").toSeq
        val jsonValues: Seq[JsString] = values.map {
          (n: Node) => JsString(n.text.trim)
        }

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
              "orientation" -> JsString( if( (node \ "@orientation").text == "horizontal") "horizontal" else "vertical" ),
              "choiceStyle" -> JsString( (node \ "@choiceStyle").text ),
              "singleChoice" -> JsBoolean( ( (node\ "@maxChoices").text == "1") )
            ),
            "prompt" -> (node \ "prompt").map(clearNamespace).text.trim,
            "choices" -> choices
        ),
        "feedback" -> feedback(node, qti),
        "correctResponse" -> correctResponse
      )

      componentId -> json

  }).toMap

}
