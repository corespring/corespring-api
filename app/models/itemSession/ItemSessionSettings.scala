package models.itemSession

import common.models.json.jerkson.{JerksonWrites, JerksonReads}
import play.api.libs.json.{JsValue, Reads}

/**
 * Configuration settings for an ItemSession
 */
case class ItemSessionSettings( var maxNoOfAttempts : Int = 1,
                                var highlightUserResponse : Boolean = true,

                                /**
                                 * Only applicable when the session is finished
                                 */
                                var highlightCorrectResponse : Boolean = true,
                                var showFeedback : Boolean = true,
                                var allowEmptyResponses : Boolean = false,
                                var submitCompleteMessage : String = ItemSessionSettings.SubmitComplete,
                                var submitIncorrectMessage : String = ItemSessionSettings.SubmitIncorrect)


object ItemSessionSettings {

  val SubmitComplete : String = "Ok! Your response was submitted."
  val SubmitIncorrect: String = "You may revise your work before you submit your final response."

  def singleTryHighlightUser() : ItemSessionSettings = {
    ItemSessionSettings(
      maxNoOfAttempts = 1,
      highlightUserResponse = true,
      highlightCorrectResponse = false,
      allowEmptyResponses = false)
  }

  implicit object Reads extends Reads[ItemSessionSettings] {

    def reads(json:JsValue) : ItemSessionSettings = {
      val default = ItemSessionSettings()
      ItemSessionSettings(
        maxNoOfAttempts = (json\"maxNoOfAttempts").asOpt[Int].getOrElse(default.maxNoOfAttempts),
        highlightCorrectResponse = (json\"highlightCorrectResponse").asOpt[Boolean].getOrElse(default.highlightCorrectResponse),
        showFeedback= (json\"showFeedback").asOpt[Boolean].getOrElse(default.showFeedback),
        highlightUserResponse = (json\"highlightUserResponse").asOpt[Boolean].getOrElse(default.highlightUserResponse),
        allowEmptyResponses = (json\"allowEmptyResponses").asOpt[Boolean].getOrElse(default.allowEmptyResponses),
        submitCompleteMessage = (json\"submitCompleteMessage").asOpt[String].getOrElse(default.submitCompleteMessage),
        submitIncorrectMessage = (json\"submitIncorrectMessage").asOpt[String].getOrElse(default.submitIncorrectMessage)
      )
    }
  }
  implicit object Writes extends JerksonWrites[ItemSessionSettings]
}


