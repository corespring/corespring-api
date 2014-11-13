package org.corespring.qtiToV2.kds

import org.corespring.qtiToV2.QtiTransformer
import org.corespring.qtiToV2.interactions._
import org.corespring.qtiToV2.kds.interactions.{ChoiceInteractionTransformer => KDSChoiceInteractionTransformer, MatchInteractionTransformer, HottextInteractionTransformer, TeacherInstructionsTransformer}

import scala.xml.Elem

object QtiTransformer extends QtiTransformer {

  def interactionTransformers(qti: Elem) = Seq(
    KDSChoiceInteractionTransformer,
    TeacherInstructionsTransformer,
    HottextInteractionTransformer,
    MatchInteractionTransformer,
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
