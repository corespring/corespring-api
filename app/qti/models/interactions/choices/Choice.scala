package qti.models.interactions.choices

import play.api.libs.json.{JsString, JsObject, JsValue, Writes}
import qti.models.interactions.FeedbackInline

trait Choice {
  val identifier:String
  val responseIdentifier:String
  val feedbackInline: Option[FeedbackInline];
  def getFeedback:Option[FeedbackInline] = feedbackInline;
}
