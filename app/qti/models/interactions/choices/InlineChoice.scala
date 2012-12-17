package qti.models.interactions.choices

import xml.Node
import qti.models.interactions.FeedbackInline

case class InlineChoice(identifier: String, responseIdentifier: String, feedbackInline: Option[FeedbackInline]) extends Choice {
  def getFeedback = feedbackInline
}

object InlineChoice {
  def apply(node: Node, responseIdentifier: String): InlineChoice = InlineChoice(
    (node \ "@identifier").text,
    responseIdentifier,
    (node \ "feedbackInline").headOption.map(FeedbackInline(_, Some(responseIdentifier)))
  )
}
