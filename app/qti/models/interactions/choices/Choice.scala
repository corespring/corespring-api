package qti.models.interactions.choices

import play.api.libs.json.{JsString, JsObject, JsValue, Writes}
import qti.models.interactions.FeedbackInline

trait Choice {
  def getFeedback: Option[FeedbackInline]
}
