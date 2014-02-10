package org.corespring.v2player.integration.transformers.qti

import play.api.libs.json.JsValue
import scala.xml.{ Elem, Node }
import scala.xml.transform.RuleTransformer
import org.corespring.v2player.integration.transformers.qti.interactions._
import play.api.libs.json.JsObject

object QtiTransformer extends XMLNamespaceClearer {

  def transform(qti: Elem): (Node, JsValue) = {

    val transformers = Seq(
      TexTransformer,
      ChoiceInteractionTransformer,
      DragAndDropInteractionTransformer,
      FeedbackBlockTransformer(qti),
      NumberedLinesTransformer(qti),
      FocusTaskInteractionTransformer,
      LineInteractionTransformer,
      OrderInteractionTransformer,
      PointInteractionTransformer,
      TextEntryInteractionTransformer,
      SelectTextInteractionTransformer,
      ExtendedTextInteractionTransformer,
      FoldableInteractionTransformer,
      CoverflowInteractionTransformer,
      CorespringTabTransformer
    )

    val statefulTransformers = Seq(
      FeedbackBlockTransformer,
      NumberedLinesTransformer
    )

    val texProcessedQti = new RuleTransformer(TexTransformer).transform(qti)
    val components = transformers.foldLeft(Map.empty[String, JsObject])(
      (map, transformer) => map ++ transformer.interactionJs(texProcessedQti.head))

    val transformedHtml = new RuleTransformer(transformers: _*).transform(texProcessedQti)
    val html = statefulTransformers.foldLeft(clearNamespace((transformedHtml.head \ "itemBody").head))(
      (html, transformer) => transformer.transform(html).head)

    (html, JsObject(components.toSeq))

  }

}

