package org.corespring.models.json.itemSession

import org.corespring.models.itemSession.ItemSessionSettings
import play.api.libs.json._

object ItemSessionSettingsFormat extends Format[ItemSessionSettings] {

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
