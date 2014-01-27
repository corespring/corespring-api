package org.corespring.v2player.integration.transformers.qti

import play.api.libs.json.JsValue
import scala.xml.{Elem, Node}
import scala.xml.transform.RuleTransformer
import org.corespring.v2player.integration.transformers.qti.interactions._
import play.api.libs.json.JsObject
import scala.collection.mutable

object QtiTransformer extends XMLNamespaceClearer {

  def transform(qti: Elem): (Node,JsValue) = {

    val transformers = Seq(
      ChoiceInteractionTransformer,
      DragAndDropInteractionTransformer,
      FeedbackBlockTransformer(qti),
      FocusTaskInteractionTransformer,
      LineInteractionTransformer,
      OrderInteractionTransformer,
      PointInteractionTransformer,
      TextEntryInteractionTransformer,
      FoldableInteractionTransformer,
      CoverflowInteractionTransformer
    )

    val components = transformers.foldLeft(Map.empty[String, JsObject])(
      (map, transformer) => map ++ transformer.interactionJs(qti))

    val transformedHtml = new RuleTransformer(transformers: _*).transform(qti)
    val html = clearNamespace((transformedHtml(0) \ "itemBody")(0))

    (html, JsObject(components.toSeq))

  }

}


