package org.corespring.poc.integration.impl.transformers.qti.interactions

import scala.xml.transform.RewriteRule
import scala.xml.{NodeSeq, Elem, Node}
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsArray
import scala.collection.mutable
import play.api.Logger

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
          Json.obj(
            "value" -> (responseDeclaration \\ "value")(0).text.trim
          )
        }

        val responseIdentifier = (e \ "@responseIdentifier").text

        val json = Json.obj(
            "componentType" -> "corespring-single-choice",
            "model" -> Json.obj(
                "config" -> Json.obj(
                  "shuffle" -> (e \ "@shuffle").text
                ),
                "prompt" -> (e \ "prompt").text.trim,
                "choices" -> choices
            ),
            "feedback" -> feedback,
            "correctResponse" -> correctResponse
          )

        componentJson.put(responseIdentifier, json)
        <corespring-single-choice id={responseIdentifier}></corespring-single-choice>
      }
      case other => other
    }
  }
}
