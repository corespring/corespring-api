package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml._
import play.api.libs.json._

object SelectTextInteractionTransformer extends InteractionTransformer {

  object Defaults {
    val shuffle = false
  }

  override def interactionJs(qti: Node): Map[String, JsObject] = (qti \\ "selectTextInteraction").map(implicit node => {
    (node \ "@responseIdentifier").text ->
      Json.obj(
        "componentType" -> "corespring-select-text",
        "model" -> Json.obj(
          "text" -> clearNamespace(node.child).mkString,
          "config" -> partialObj(
            "selectionUnit" -> optForAttr[JsString]("selectionType"),
            "checkIfCorrect" -> optForAttr[JsString]("checkIfCorrect"),
            "minSelections" -> optForAttr[JsNumber]("minSelections"),
            "maxSelections" -> optForAttr[JsNumber]("maxSelections")
          )
        )
      )
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == "selectTextInteraction" => {
      val identifier = (elem \ "@responseIdentifier").text
        <corespring-select-text id={identifier}>{clearNamespace(elem.child)}</corespring-select-text>
    }
    case _ => node
  }

}
