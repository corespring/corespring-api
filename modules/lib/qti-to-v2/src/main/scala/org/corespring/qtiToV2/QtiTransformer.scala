package org.corespring.qtiToV2

import org.corespring.common.xml.XMLNamespaceClearer
import org.corespring.qtiToV2.customScoring.CustomScoringTransformer
import org.corespring.qtiToV2.interactions._
import play.api.libs.json._

import scala.xml.{Node, Elem}
import scala.xml.transform.RuleTransformer

trait QtiTransformer extends XMLNamespaceClearer {

  val scoringTransformer = new CustomScoringTransformer

  def interactionTransformers(qti: Elem): Seq[InteractionTransformer]
  def statefulTransformers: Seq[Transformer]

  def customScoring(qti: Node, components: Map[String, JsObject]): JsObject = {
    val typeMap = components.map { case (k, v) => (k -> (v \ "componentType").as[String]) }
    (qti \\ "responseProcessing").headOption.map { rp =>
      scoringTransformer.generate(rp.text, components, typeMap) match {
        case Left(e) => throw e
        case Right(js) => Json.obj("customScoring" -> js)
      }
    }.getOrElse(Json.obj())
  }

  def transform(qti: Elem): JsValue = {

    val transformers = interactionTransformers(qti)

    /** Need to pre-process Latex so that it is available for all JSON and XML transformations **/
    val texProcessedQti = new RuleTransformer(TexTransformer).transform(qti)

    val components = transformers.foldLeft(Map.empty[String, JsObject])(
      (map, transformer) => map ++ transformer.interactionJs(texProcessedQti.head))

    val transformedHtml = new RuleTransformer(transformers: _*).transform(texProcessedQti)
    val html = statefulTransformers.foldLeft(clearNamespace((transformedHtml.head \ "itemBody").head))(
      (html, transformer) => transformer.transform(html).head)

    components.foreach{ case (id, json) => println(id); println(Json.prettyPrint(json)) }

    Json.obj(
      "xhtml" -> html.toString,
      "components" -> components) ++ customScoring(qti, components)
  }

}

object QtiTransformer extends QtiTransformer {

  def interactionTransformers(qti: Elem) = Seq(
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

  def statefulTransformers = Seq(
    FeedbackBlockTransformer,
    NumberedLinesTransformer,
    TextEntryInteractionTransformer)

}
