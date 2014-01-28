package org.corespring.v2player.integration.transformers.qti.interactions

import play.api.libs.json._
import scala.xml.{Elem, Node}
import play.api.libs.json.JsString

object FocusTaskInteractionTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node) = (qti \\ "focusTaskInteraction").map(implicit node => {
    (node \\ "@responseIdentifier").text ->
      Json.obj(
        "componentType" -> "corespring-focus-task",
        "correctResponse" -> Json.obj(
          "value" -> (responseDeclaration(node, qti) \\ "value").map(_.text)
        ),
        "model" -> partialObj(
          "prompt" -> (node \ "prompt").headOption.map(n => JsString(n.text)),
          "config" -> Some(partialObj(
            "shuffle" -> optForAttr[JsBoolean]("shuffle"),
            "itemShape" -> optForAttr[JsString]("itemShape"),
            "checkIfCorrect" -> optForAttr[JsString]("checkIfCorrect"),
            "minSelections" -> optForAttr[JsNumber]("minSelections"),
            "maxSelections" -> optForAttr[JsNumber]("maxSelections")
          )),
          "choices" -> Some(JsArray(
            (node \\ "focusChoice").map(choiceNode =>
              Json.obj(
                "label" -> choiceNode.text,
                "value" -> (choiceNode \ "@identifier").text
              )
            )
          ))
        )
      )
    }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "focusTaskInteraction" => {
      val identifier = (e \ "@responseIdentifier").text
      <corespring-focus-task id={identifier} />
    }
    case _ => node
  }

}
