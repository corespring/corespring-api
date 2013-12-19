package org.corespring.qti.models.interactions.choices

import org.corespring.qti.models.interactions.FeedbackInline
import xml.Node

case class InlineChoice(identifier: String, responseIdentifier: String, feedbackInline: Option[FeedbackInline]) extends Choice

object InlineChoice {
  def apply(node: Node, responseIdentifier: String): InlineChoice = InlineChoice(
    (node \ "@identifier").text,
    responseIdentifier,
    (node \ "feedbackInline").headOption.map(FeedbackInline(_, Some(responseIdentifier))))
}
