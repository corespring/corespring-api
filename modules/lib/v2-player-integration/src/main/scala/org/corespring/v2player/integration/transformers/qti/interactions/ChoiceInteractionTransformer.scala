package org.corespring.v2player.integration.transformers.qti.interactions

import org.corespring.qti.models.QtiItem
import play.api.Logger
import play.api.libs.json._
import scala.collection.mutable
import scala.xml.transform.RewriteRule
import scala.xml.{Text, Elem, Node}

class ChoiceInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node)
  extends RewriteRule
  with InteractionTransformer {

  private val logger : Logger = Logger("poc.integration")

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if Seq("choiceInteraction", "inlineChoiceInteraction").contains(e.label) => {

      val qtiItem = QtiItem(qti)

      val componentId = (e \ "@responseIdentifier").text.trim

      logger.debug( s"transform choiceInteraction: ${componentId}")

      def choices: JsArray = {
        val out : Seq[JsValue] = ((e \\ "simpleChoice").toSeq ++ (e \\ "inlineChoice")).map{ n: Node =>
          Json.obj(
            "label" -> n.child.filter(_.isInstanceOf[Text]).mkString,
            "value" -> (n \ "@identifier").text.trim
          )
        }
        JsArray(out)
      }

      def correctResponse: JsObject = {
        val values: Seq[Node] = (responseDeclaration(e, qti) \\ "value").toSeq
        val jsonValues: Seq[JsString] = values.map {
          (n: Node) => JsString(n.text.trim)
        }

        Json.obj("value" -> JsArray(jsonValues))
      }

      val responseIdentifier = (e \ "@responseIdentifier").text

      val json = Json.obj(
          "componentType" -> (e.label match {
            case "choiceInteraction" => "corespring-multiple-choice"
            case "inlineChoiceInteraction" => "corespring-inline-choice"
            case _ => throw new IllegalStateException
          })
        ,
          "model" -> Json.obj(
              "config" -> Json.obj(
                "shuffle" -> (e \ "@shuffle").text,
                "orientation" -> JsString( if( (e \ "@orientation").text == "vertical") "vertical" else "horizontal" ),
                "singleChoice" -> JsBoolean( ( (e\ "@maxChoices").text == "1") )
              ),
              "prompt" -> (e \ "prompt").map(clearNamespace).text.trim,
              "choices" -> choices
          ),
          "feedback" -> feedback(node, qti),
          "correctResponse" -> correctResponse
        )

      componentJson.put(responseIdentifier, json)
      e.label match {
        case "choiceInteraction" => <corespring-multiple-choice id={responseIdentifier}></corespring-multiple-choice>
        case "inlineChoiceInteraction" => <corespring-inline-choice id={responseIdentifier}></corespring-inline-choice>
        case _ => throw new IllegalStateException
      }

    }
    case other => other
  }

}
