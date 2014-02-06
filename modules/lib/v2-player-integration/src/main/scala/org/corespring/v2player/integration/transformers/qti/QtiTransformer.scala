package org.corespring.v2player.integration.transformers.qti

import play.api.libs.json.JsValue
import scala.xml.{ Elem, Node }
import scala.xml.transform.RuleTransformer
import org.corespring.v2player.integration.transformers.qti.interactions._
import play.api.libs.json.JsObject

object QtiTransformer extends XMLNamespaceClearer {

  def transform(qti: Elem): (Node, JsValue) = {

    val transformers = Seq(
      ChoiceInteractionTransformer,
      DragAndDropInteractionTransformer,
      FeedbackBlockTransformer(qti),
      FocusTaskInteractionTransformer,
      LineInteractionTransformer,
      OrderInteractionTransformer,
      PointInteractionTransformer,
      TextEntryInteractionTransformer,
      SelectTextInteractionTransformer,
      ExtendedTextInteractionTransformer,
      TexTransformer,
      FoldableInteractionTransformer,
      CoverflowInteractionTransformer,
      CorespringTabTransformer)

    val components = transformers.foldLeft(Map.empty[String, JsObject])(
      (map, transformer) => map ++ transformer.interactionJs(qti))

    val transformedHtml = new RuleTransformer(transformers: _*).transform(qti)
    val html = FeedbackBlockTransformer.transform(clearNamespace((transformedHtml(0) \ "itemBody")(0)))

    (html, JsObject(components.toSeq))

  }

}

