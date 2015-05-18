package org.corespring.qtiToV2.kds

import org.corespring.qtiToV2.{QtiTransformer => SuperQtiTransformer}
import org.corespring.qtiToV2.interactions._
import org.corespring.qtiToV2.kds.interactions.{ChoiceInteractionTransformer => KDSChoiceInteractionTransformer, TextEntryInteractionTransformer => KDSTextEntryInteractionTransformer, _}
import play.api.libs.json._

import scala.xml.{Node, Elem}

object QtiTransformer extends SuperQtiTransformer with ProcessingTransformer {

  override def customScoring(qti: Node, components: Map[String, JsObject]): JsObject = {
    toJs(qti).map(wrap) match {
      case Some(javascript) => Json.obj("customScoring" -> javascript)
      case _ => Json.obj()
    }
  }

  def interactionTransformers(qti: Elem) = Seq(
    SelectPointInteractionTransformer(qti),
    KDSChoiceInteractionTransformer,
    TeacherInstructionsTransformer,
    HottextInteractionTransformer,
    RubricBlockTransformer,
    ElementTransformer,
    MatchInteractionTransformer,
    NumberLineInteractionTransformer,
    DragAndDropInteractionTransformer,
    FeedbackBlockTransformer(qti),
    NumberedLinesTransformer(qti),
    FocusTaskInteractionTransformer,
    KDSTextEntryInteractionTransformer(qti),
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
    KDSTextEntryInteractionTransformer)

}
