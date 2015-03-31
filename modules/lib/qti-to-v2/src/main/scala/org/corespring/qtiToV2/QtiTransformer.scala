package org.corespring.qtiToV2

import org.corespring.common.xml.XMLNamespaceClearer
import org.corespring.qtiToV2.customScoring.CustomScoringTransformer
import org.corespring.qtiToV2.interactions._
import play.api.libs.json._

import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node}

object QtiTransformer extends XMLNamespaceClearer {

  val scoringTransformer = new CustomScoringTransformer

  object ItemBodyTransformer extends RewriteRule with XMLNamespaceClearer{

    override def transform(node: Node): Seq[Node] = {
      node match {
        case elem: Elem if elem.label == "itemBody" => {
          <div class="item-body">{elem.child}</div>
        }
        case _ => node
      }
    }
  }

  def transform(qti: Elem): JsValue = {

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
      CorespringTabTransformer )

    val statefulTransformers: Seq[Transformer] = Seq(
      FeedbackBlockTransformer,
      NumberedLinesTransformer,
      TextEntryInteractionTransformer)

    /** Need to pre-process Latex so that it is available for all JSON and XML transformations **/
    val texProcessedQti = new RuleTransformer(FontTransformer).transform(new RuleTransformer(TexTransformer).transform(qti))
    val components = transformers.foldLeft(Map.empty[String, JsObject])(
      (map, transformer) => map ++ transformer.interactionJs(texProcessedQti.head))

    val transformedHtml = new RuleTransformer(transformers : _*).transform(texProcessedQti)
    val html = statefulTransformers.foldLeft(clearNamespace((transformedHtml.head \ "itemBody").head))(
      (html, transformer) => transformer.transform(html).head)

    val typeMap = components.map { case (k, v) => (k -> (v \ "componentType").as[String]) }

    val customScoring = (qti \\ "responseProcessing").headOption.map { rp =>
      scoringTransformer.generate(rp.text, components, typeMap) match {
        case Left(e) => throw e
        case Right(js) => Json.obj("customScoring" -> js)
      }
    }.getOrElse(Json.obj())

    val divRoot = new RuleTransformer(ItemBodyTransformer).transform(html).head

    Json.obj(
      "xhtml" -> divRoot.toString,
      "components" -> components) ++ customScoring
  }

}
