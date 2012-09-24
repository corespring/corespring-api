package models

import play.api.libs.json._
import models.CorrectResponseMultiple
import play.api.libs.json.JsString
import models.CorrectResponseSingle
import scala.xml._


/**
 * data sent back after a session update (representing a completed item). example:
 *
 * {
 *         sessionData: {
 *             feedbackContents: {
 *                 [csFeedbackId]: "[contents of feedback element]",
 *                 [csFeedbackId]: "[contents of feedback element]"
 *             }
 *             correctResponse: {
 *                 irishPresident: "higgins",
 *                 rainbowColors: ['blue','violet', 'red']
 *             }
 *         }
 *   }
 * @param feedbackContents map of csFeedbackId's to outcome value's
 * @param correctResponses array of CorrectResponse objects, representing the mapping of response identifier's to correct response
 */
case class SessionData(var feedbackContents:Map[String,String] = Map(), var correctResponses: CorrectResponses = CorrectResponses())
object SessionData{
  val feedbackContents = "feedbackContents"
  val correctResponses = "correctResponses"
  implicit object SessionDataWrites extends Writes[SessionData]{
    def writes(sd: SessionData) = {
      JsObject(Seq(
        feedbackContents -> Json.toJson(sd.feedbackContents),
        correctResponses -> Json.toJson(sd.correctResponses)
      ))
    }
  }
  def apply(elem:Elem):SessionData = {
    val
  }
}
/*******************move this to qti item somehow*******************/
case class ResponseDeclaration(identifier:String, cardinality:String,correctResponse:CorrectResponse)
object ResponseDeclaration{

}
trait Interaction{
  val identifier:String
}
trait CorrectResponse
case class CorrectResponseSingle(identifier: String, value: String) extends CorrectResponse
case class CorrectResponseMultiple(identifier: String, value: Seq[String]) extends CorrectResponse

object Interaction{
  val choiceInteraction = "choiceInteraction"
  def apply(node:Node):Interaction = {
    node.label match {
      case `choiceInteraction` =>
      case _ => throw new RuntimeException("uknown interaction")
    }
  }
}
case class ChoiceInteraction(identifier:String, choices:Seq[SimpleChoice]) extends Interaction
object ChoiceInteraction{
  def apply(node:Node):ChoiceInteraction = ChoiceInteraction(
      (node \ "@identifier").text,
      (node \ "simpleChoice").map(SimpleChoice(_))
    )
}
case class SimpleChoice(identifier: String, feedbackInline: Option[FeedbackInline])
object SimpleChoice{
  def apply(node:Node):SimpleChoice = SimpleChoice(
    (node \ "@identifier").text,
    (node \ "feedbackInline").headOption.map(FeedbackInline(_))
  )
}
case class FeedbackInline(csFeedbackId:String, content: String)
object FeedbackInline{
  def apply(node:Node):FeedbackInline = FeedbackInline(
    (node \ "@csFeedbackId").text,
    node.child.text
  )
}
/*********************************************************************/
case class CorrectResponses(var correctResponses:Seq[CorrectResponse] = Seq())
object CorrectResponses{
  implicit object CorrectResponsesWrites extends Writes[CorrectResponses]{
    def writes(crs: CorrectResponses) = {
      var crseq:Seq[(String,JsValue)] = Seq()
      crs.correctResponses.foreach(cr => {
        cr match {
          case single:CorrectResponseSingle => crseq = crseq :+ (single.identifier -> JsString(single.value))
          case multiple:CorrectResponseMultiple => crseq = crseq :+ (multiple.identifier -> JsArray(multiple.value.map(JsString(_))))
        }
      })
      JsObject(crseq)
    }
  }
}

