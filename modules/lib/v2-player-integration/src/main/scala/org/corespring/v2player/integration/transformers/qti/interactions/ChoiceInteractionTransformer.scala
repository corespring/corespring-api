package org.corespring.v2player.integration.transformers.qti.interactions

import org.corespring.qti.models.QtiItem
import play.api.Logger
import play.api.libs.json._
import scala.collection.mutable
import scala.xml.transform.RewriteRule
import scala.xml.{Elem, Node}

class ChoiceInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node)
  extends RewriteRule
  with InteractionTransformer {

  private val logger : Logger = Logger("poc.integration")

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "choiceInteraction" => {

      val qtiItem = QtiItem(qti)
      val componentId = (e \ "@responseIdentifier").text.trim

      logger.debug( s"transform choiceInteraction: ${componentId}")

      def choices : JsArray = {
        val out : Seq[JsValue] = (e \\ "simpleChoice").toSeq.map{ n : Node =>
          Json.obj(
            "label" -> JsString(n.text),
            "value" -> JsString((n \ "@identifier").text.trim)
          )
        }
        JsArray(out)
      }

      def feedback : JsArray = {

        val choiceIds : Seq[Node] = (e \\ "simpleChoice").toSeq ++ (e \\ "inlineChoice").toSeq

        val feedbackObjects : Seq[JsObject] = choiceIds.map{ (n:Node) =>

          val id = (n \ "@identifier").text.trim

          val fbInline = qtiItem.getFeedback(componentId, id)

          fbInline.map{ fb =>
            val content = if( fb.defaultFeedback ){
              fb.defaultContent(qtiItem)
            } else {
              fb.content
            }
            Json.obj( "value" -> JsString(id), "feedback" -> JsString(content))
          }
        }.flatten
        JsArray(feedbackObjects)
      }

      def correctResponse : JsObject = {

        val values: Seq[Node] = (responseDeclaration(e, qti) \\ "value").toSeq

        val jsonValues: Seq[JsString] = values.map {
          (n: Node) => JsString(n.text.trim)
        }

        Json.obj("value" -> JsArray(jsonValues))
      }

      val responseIdentifier = (e \ "@responseIdentifier").text

      val json = Json.obj(
          "componentType" -> "corespring-multiple-choice",
          "model" -> Json.obj(
              "config" -> Json.obj(
                "shuffle" -> (e \ "@shuffle").text,
                "orientation" -> JsString( if( (e \ "@orientation").text == "vertical") "vertical" else "horizontal" ),
                "singleChoice" -> JsBoolean( ( (e\ "@maxChoices").text == "1") )
              ),
              "prompt" -> (e \ "prompt").map(clearNamespace).text.trim,
              "choices" -> choices
          ),
          "feedback" -> feedback,
          "correctResponse" -> correctResponse
        )

      componentJson.put(responseIdentifier, json)
      <corespring-multiple-choice id={responseIdentifier}></corespring-multiple-choice>
    }
    case other => other
  }

}
