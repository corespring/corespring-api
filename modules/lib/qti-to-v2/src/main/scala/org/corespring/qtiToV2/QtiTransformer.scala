package org.corespring.qtiToV2

import org.corespring.common.xml.XMLNamespaceClearer
import org.corespring.qtiToV2.customScoring.CustomScoringTransformer
import org.corespring.qtiToV2.interactions._

import scala.xml.transform.RuleTransformer
import scala.xml.{ Elem, Node }

import play.api.libs.json._
object QtiTransformer extends XMLNamespaceClearer {

  def transform(qti: Elem): (Node, JsValue) = {

    val transformers = Seq(
      ChoiceInteractionTransformer,
      DragAndDropInteractionTransformer,
      FeedbackBlockTransformer(qti),
      NumberedLinesTransformer(qti),
      FocusTaskInteractionTransformer,
      TextEntryInteractionTransformer(qti),
      LineInteractionTransformer,
      OrderInteractionTransformer,
      PointInteractionTransformer,
      SelectTextInteractionTransformer,
      ExtendedTextInteractionTransformer,
      FoldableInteractionTransformer,
      CoverflowInteractionTransformer,
      CorespringTabTransformer)

    val statefulTransformers: Seq[Transformer] = Seq(
      FeedbackBlockTransformer,
      NumberedLinesTransformer,
      TextEntryInteractionTransformer)

    /** Need to pre-process Latex so that it is avaiable for all JSON and XML transformations **/
    val texProcessedQti = new RuleTransformer(TexTransformer).transform(qti)
    val components = transformers.foldLeft(Map.empty[String, JsObject])(
      (map, transformer) => map ++ transformer.interactionJs(texProcessedQti.head))

    val transformedHtml = new RuleTransformer(transformers: _*).transform(texProcessedQti)
    val html = statefulTransformers.foldLeft(clearNamespace((transformedHtml.head \ "itemBody").head))(
      (html, transformer) => transformer.transform(html).head)

    val customScoring = (qti \\ "responseProcessing").headOption.map { rp =>
      val wrappedJs = new CustomScoringTransformer().generate(rp.text, components)
      Json.obj("customScoring" -> wrappedJs)
    }.getOrElse(Json.obj())

    (html, JsObject(components.toSeq) ++ customScoring)

  }

}

