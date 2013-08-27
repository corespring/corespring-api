package org.corespring.platform.core.models.itemSession

import play.api.libs.json._
import play.api.libs.json.JsObject

/**
 * Configuration settings for an ItemSession
 */
case class ItemSessionSettings(var maxNoOfAttempts: Int = 1,
  var highlightUserResponse: Boolean = true,

  /**
   * Only applicable when the session is finished
   */
  var highlightCorrectResponse: Boolean = true,
  var showFeedback: Boolean = true,
  var allowEmptyResponses: Boolean = false,
  var submitCompleteMessage: String = ItemSessionSettings.SubmitComplete,
  var submitIncorrectMessage: String = ItemSessionSettings.SubmitIncorrect)

object ItemSessionSettings {

  val SubmitComplete: String = "Ok! Your response was submitted."
  val SubmitIncorrect: String = "You may revise your work before you submit your final response."

  def singleTryHighlightUser(): ItemSessionSettings = {
    ItemSessionSettings(
      maxNoOfAttempts = 1,
      highlightUserResponse = true,
      highlightCorrectResponse = false,
      allowEmptyResponses = false)
  }

  implicit object Reads extends Reads[ItemSessionSettings] {

    override def reads(json: JsValue): JsResult[ItemSessionSettings] = {
      val default = ItemSessionSettings()
      JsSuccess(ItemSessionSettings(
        (json \ "maxNoOfAttempts").asOpt[Int].getOrElse(default.maxNoOfAttempts),
        (json \ "highlightUserResponse").asOpt[Boolean].getOrElse(default.highlightUserResponse),
        (json \ "highlightCorrectResponse").asOpt[Boolean].getOrElse(default.highlightCorrectResponse),
        (json \ "showFeedback").asOpt[Boolean].getOrElse(default.showFeedback),
        (json \ "allowEmptyResponses").asOpt[Boolean].getOrElse(default.allowEmptyResponses),
        (json \ "submitCompleteMessage").asOpt[String].getOrElse(default.submitCompleteMessage),
        (json \ "submitIncorrectMessage").asOpt[String].getOrElse(default.submitIncorrectMessage)))
    }
  }
  implicit object Writes extends Writes[ItemSessionSettings] {
    def writes(settings: ItemSessionSettings): JsValue = {
      JsObject(Seq(
        "maxNoOfAttempts" -> JsNumber(settings.maxNoOfAttempts),
        "highlightUserResponse" -> JsBoolean(settings.highlightUserResponse),
        "highlightCorrectResponse" -> JsBoolean(settings.highlightCorrectResponse),
        "showFeedback" -> JsBoolean(settings.showFeedback),
        "allowEmptyResponses" -> JsBoolean(settings.allowEmptyResponses),
        "submitCompleteMessage" -> JsString(settings.submitCompleteMessage),
        "submitIncorrectMessage" -> JsString(settings.submitIncorrectMessage)))
    }
  }
}

