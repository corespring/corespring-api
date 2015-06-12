package org.corespring.qtiToV2

import org.corespring.common.xml.XMLNamespaceClearer
import org.corespring.qtiToV2.customScoring.CustomScoringTransformer
import org.corespring.qtiToV2.interactions._
import play.api.libs.json._

import scala.xml._
import scala.xml.transform.{ RewriteRule, RuleTransformer }

/**
 * ***** DO NOT CHANGE THE PUBLIC METHODS EXPOSED BY THIS OBJECT ******
 *                                                                     *
 *     This object is being overwritten in the kds-qti-converter       *
 *     branch for KDS-specific transformations. If you alter the       *
 *     way this object interacts with other code, it becomes           *
 *     EXTREMELY difficult and time consuming to support KDS. This     *
 *     will be fixed in the near future, but for the time being        *
 *     PLEASE DO NOT alter the public-facing methods.                  *
 *                                                                     *
 *     Yes, this is terrible. This will be fixed soon, as the v2       *
 *     transformation code will be moved into a separate jar and       *
 *     imported by cs-api and kds-processor. For more info see         *
 *     https://github.com/corespring/kds-processor#next-steps          *
 *                                                                     *
 * *********************************************************************
 */
trait QtiTransformer extends XMLNamespaceClearer {

  implicit class NodeWithClass(node: Node) {
    def withClass(classString: String) = node match {
      case node: Elem => node.copy(child = node.child, label = "div") % Attribute(None, "class", Text(classString), Null)
      case _ => throw new Exception("Cannot add class to non-Elem node.")
    }
  }

  val scoringTransformer = new CustomScoringTransformer

  object ItemBodyTransformer extends RewriteRule with XMLNamespaceClearer {

    override def transform(node: Node): Seq[Node] = {
      node match {
        case elem: Elem if elem.label == "itemBody" => {
          <div class="item-body">{ elem.child }</div>
        }
        case _ => node
      }
    }
  }

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

    /** Need to pre-process Latex so that it is avaiable for all JSON and XML transformations **/
    val texProcessedQti = new RuleTransformer(FontTransformer).transform(new RuleTransformer(TexTransformer).transform(qti))
    val components = transformers.foldLeft(Map.empty[String, JsObject])(
      (map, transformer) => map ++ transformer.interactionJs(texProcessedQti.head))

    val transformedHtml = new RuleTransformer(transformers: _*).transform(texProcessedQti)
    val html = statefulTransformers.foldLeft(clearNamespace((transformedHtml.head \ "itemBody").head))(
      (html, transformer) => transformer.transform(html).head)

    val divRoot = new RuleTransformer(ItemBodyTransformer).transform(html).head

    Json.obj(
      "xhtml" -> divRoot.toString,
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
    GraphicGapMatchInteractionTransformer,
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