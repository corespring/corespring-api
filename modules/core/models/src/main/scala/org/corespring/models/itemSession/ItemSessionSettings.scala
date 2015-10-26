package org.corespring.models.itemSession

/**
 * Configuration settings for an ItemSession
 */
case class ItemSessionSettings(
  maxNoOfAttempts: Int = 1,
  highlightUserResponse: Boolean = true,
  /**
   * Only applicable when the session is finished
   */
  highlightCorrectResponse: Boolean = true,
  showFeedback: Boolean = true,
  allowEmptyResponses: Boolean = false,
  submitCompleteMessage: String = ItemSessionSettings.SubmitComplete,
  submitIncorrectMessage: String = ItemSessionSettings.SubmitIncorrect)

object ItemSessionSettings {

  val SubmitComplete: String = "Ok! Your response was submitted."
  val SubmitIncorrect: String = "You may revise your work before you submit your final response."
}

