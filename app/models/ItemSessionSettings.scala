package models

import play.api.libs.json._

/**
 * Configuration settings for an ItemSession
 */
case class ItemSessionSettings( var maxNoOfAttempts : Int = 0,
                                var highlightUserResponse : Boolean = true,

                                /**
                                 * Only applicable when the session is finished
                                 */
                                var highlightCorrectResponse : Boolean = true,
                                var showFeedback : Boolean = true,
                                var allowEmptyResponses : Boolean = false,
                                var submitCompleteMessage : String = ItemSessionSettings.SubmitComplete,
                                var submitIncorrectMessage : String = ItemSessionSettings.SubmitIncorrect) {
  /**
   * Ensure that highlightCorrectResponse is never true when
   * infinite attempts are allowed.
   */
  highlightCorrectResponse = if (maxNoOfAttempts == 0) false else highlightCorrectResponse
}

object ItemSessionSettings {

  val SubmitComplete : String = "Submit Completed"
  val SubmitIncorrect: String = "Submit Incorrect"



  implicit object Reads extends Reads[ItemSessionSettings] {
    def reads( js : JsValue) : ItemSessionSettings = {
      val string = Json.stringify(js)
      com.codahale.jerkson.Json.parse[ItemSessionSettings](string)
    }
  }

  implicit object Writes extends Writes[ItemSessionSettings] {
    def writes( settings : ItemSessionSettings ) : JsValue = {
      val string = com.codahale.jerkson.Json.generate(settings)
      Json.parse(string)
    }
  }
}


