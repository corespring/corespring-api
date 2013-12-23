package org.corespring.qti.models.interactions.choices

import org.corespring.qti.models.interactions.FeedbackInline
import xml.Node

case class SimpleChoice(identifier: String, responseIdentifier: String, feedbackInline: Option[FeedbackInline]) extends Choice

object SimpleChoice {
  def apply(node: Node, responseIdentifier: String): SimpleChoice = SimpleChoice(
    (node \ "@identifier").text,
    responseIdentifier,
    (node \ "feedbackInline").headOption.map(FeedbackInline(_, Some(responseIdentifier))))
}
