package models

import play.api.libs.json._
import common.models.json.jerkson.{JerksonWrites, JerksonReads}

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
  val SubmitIncorrect: String = "You may revise your work before you submit it."

  implicit object Reads extends JerksonReads[ItemSessionSettings] {
    def manifest = Manifest.classType( new ItemSessionSettings().getClass)
  }
  implicit object Writes extends JerksonWrites[ItemSessionSettings]
}


