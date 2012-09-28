package models

import play.api.libs.json._
import play.api.libs.json.JsString
import scala.xml._
import models.bleezmo._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString


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
 */
case class SessionData(qtiItem: bleezmo.QtiItem)
object SessionData{
  implicit object SessionDataWrites extends Writes[SessionData]{
    def writes(sd: SessionData) = {
      var correctResponses:Seq[(String,JsValue)] = Seq()
      sd.qtiItem.responseDeclarations.foreach(rd => {
        rd.correctResponse.foreach( _ match {
          case crs:CorrectResponseSingle => correctResponses = correctResponses :+ (rd.identifier -> JsString(crs.value))
          case crm:CorrectResponseMultiple => correctResponses = correctResponses :+ (rd.identifier -> JsArray(crm.value.map(JsString(_))))
          case cro:CorrectResponseOrdered => correctResponses = correctResponses :+ (rd.identifier -> JsArray(cro.value.map(JsString(_))))
          case _ => throw new RuntimeException("unexpected correct response type")
        })
      })
      var feedbackContents:Seq[(String,JsValue)] = Seq()
      sd.qtiItem.itemBody.interactions.foreach(interaction => {
        interaction match {
          case ci:ChoiceInteraction => ci.choices.map(choice => choice.feedbackInline.foreach(fi =>
            feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(if(fi.defaultFeedback) fi.defaultContent(sd.qtiItem)
              else fi.content))
          ))
          case oi:OrderInteraction => oi.choices.map(choice => choice.feedbackInline.foreach(fi =>
            feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(if(fi.defaultFeedback) fi.defaultContent(sd.qtiItem)
            else fi.content))
          ))
        }
      })
      sd.qtiItem.itemBody.feedbackInlines.foreach(fi =>
        feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(if(fi.defaultFeedback) fi.defaultContent(sd.qtiItem)
        else fi.content))
      )
      JsObject(Seq(
        "feedbackContents" -> JsObject(feedbackContents),
        "correctResponses" -> JsObject(correctResponses)
      ))
    }
  }
  implicit object SessionDataReads extends Reads[SessionData]{
    def reads(json:JsValue):SessionData = {
      val feedbackfields = (json \ "feedbackContents").asInstanceOf[JsObject].fields
      SessionData(null)
    }
  }
}


