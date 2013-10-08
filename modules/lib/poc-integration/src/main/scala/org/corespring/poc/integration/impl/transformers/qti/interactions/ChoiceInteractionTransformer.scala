package org.corespring.poc.integration.impl.transformers.qti.interactions

import play.api.Logger
import play.api.libs.json._
import scala.collection.mutable
import scala.xml.transform.RewriteRule
import scala.xml.{NodeSeq, Elem, Node}

object ChoiceInteractionTransformer {

  private val logger : Logger = Logger("poc.integration")

  class Rewriter(componentJson:mutable.Map[String,JsObject], responseDeclarations:NodeSeq) extends RewriteRule {

    override def transform(node: Node): Seq[Node] = node match {
      case e: Elem if e.label == "choiceInteraction" => {

        logger.debug( s"transform choiceInteraction: ${(e \ "@responseIdentifier").text }")

        def choices : JsArray = {
          val out : Seq[JsValue] = (e \\ "simpleChoice").toSeq.map{ n : Node =>
            Json.obj(
              "label" -> JsString(n.text),
              "value" -> JsString( (n \ "@identifier").text.trim )
            )
          }
          JsArray(out)
        }

        def feedback : JsArray = {
          val out : Seq[JsValue] = (e \\ "feedbackInline").toSeq.map{ n : Node =>
            Json.obj("value" -> JsString((n \ "@identifier").text), "feedback" -> "TODO")
          }
          JsArray(out)
        }

        def responseDeclaration : Node = {
          responseDeclarations.find{ (rd : Node) =>
            val responseId = (rd \ "@identifier").text
            val elementId = (e \ "@responseIdentifier").text
            logger.debug( s"$responseId ==? $elementId" )
            responseId == elementId
          }.getOrElse(throw new RuntimeException("No response identifier??"))
        }

        def correctResponse : JsObject = {

          val values: Seq[Node] = (responseDeclaration \\ "value").toSeq

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
                "prompt" -> (e \ "prompt").text.trim,
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
}
