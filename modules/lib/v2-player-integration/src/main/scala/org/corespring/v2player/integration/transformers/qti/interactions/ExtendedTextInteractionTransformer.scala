package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.{Elem, Node}
import play.api.libs.json.{JsNumber, Json, JsObject}

object ExtendedTextInteractionTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node): Map[String, JsObject] =
    (qti \\ "extendedTextInteraction").map(implicit node => {
      (node \\ "@responseIdentifier").text -> Json.obj(
        "componentType" -> "corespring-extended-text-entry",
        "model" -> Json.obj(
          "config" -> partialObj(
            "expectedLength" -> optForAttr[JsNumber]("expectedLength"),
            "expectedLines" -> optForAttr[JsNumber]("expectedLines"),
            "maxStrings" -> optForAttr[JsNumber]("maxStrings"),
            "minStrings" -> optForAttr[JsNumber]("minStrings")
          )
        )
      )
    }).toMap

  override def transform(node: Node) = node match {
    case e: Elem if (e.label == "extendedTextInteraction") =>
      <corespring-extended-text-entry id={(e \ "@responseIdentifier").text}></corespring-extended-text-entry>
    case _ => node
  }

}
