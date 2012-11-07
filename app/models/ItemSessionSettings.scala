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
                                var showFeedback : Boolean = false,
                                var allowEmptyResponses : Boolean = false,
                                var submitCompleteMessage : String = ItemSessionSettings.SubmitComplete,
                                var submitIncorrectMessage : String = ItemSessionSettings.SubmitIncorrect)


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


