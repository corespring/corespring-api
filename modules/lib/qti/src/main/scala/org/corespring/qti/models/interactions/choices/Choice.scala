package org.corespring.qti.models.interactions.choices

import org.corespring.qti.models.interactions.FeedbackInline

trait Choice {
  val identifier: String
  val responseIdentifier: String
  val feedbackInline: Option[FeedbackInline]
  def getFeedback: Option[FeedbackInline] = feedbackInline
}
