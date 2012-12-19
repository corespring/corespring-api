package qti.models.interactions.choices

import xml.Node
import qti.models.interactions.FeedbackInline

case class SimpleChoice(identifier: String, responseIdentifier: String, feedbackInline: Option[FeedbackInline]) extends Choice

object SimpleChoice {
  def apply(node: Node, responseIdentifier: String): SimpleChoice = SimpleChoice(
    (node \ "@identifier").text,
    responseIdentifier,
    (node \ "feedbackInline").headOption.map(FeedbackInline(_, Some(responseIdentifier)))
  )
}
