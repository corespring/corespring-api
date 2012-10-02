package models

import play.api.libs.json._
import play.api.libs.json.JsString
import scala.xml._
import models.bleezmo._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import collection.immutable.HashMap


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
case class SessionData(qtiItem: bleezmo.QtiItem,responses:Seq[ItemResponse])
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

      def filterFeedbacks(feedbacks:Seq[FeedbackInline]):Seq[FeedbackInline] = {
        var feedbackGroups:HashMap[String,Seq[FeedbackInline]] = HashMap()
        feedbacks.foreach(fi => if (feedbackGroups.get(fi.responseIdentifier).isDefined){
          feedbackGroups += (fi.responseIdentifier -> (feedbackGroups.get(fi.responseIdentifier).get :+ fi))
        }else{
          feedbackGroups += (fi.responseIdentifier -> Seq(fi))
        })
        feedbackGroups.map(kvpair => filterFeedbackGroup(kvpair._2)).flatten.toSeq
      }
      def filterFeedbackGroup(feedbackGroup:Seq[FeedbackInline]):Option[FeedbackInline] = {
        feedbackGroup.find(fi => sd.responses.find(_.value == fi.identifier).isDefined) match {
          case Some(fi) => Some(fi)
          case None => feedbackGroup.find(fi => fi.incorrectResponse)
        }
      }

      def getFeedbackContent(fi:FeedbackInline) = if(fi.defaultFeedback)
        fi.defaultContent(sd.qtiItem)
      else fi.content

      var feedbackContents:Seq[(String,JsValue)] = Seq()
      sd.qtiItem.itemBody.interactions.foreach(interaction => {
        interaction match {
          case ci:ChoiceInteraction => filterFeedbackGroup(ci.choices.filter(_.feedbackInline.isDefined).map(_.feedbackInline.get))
            .foreach(fi => feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi))))
          case oi:OrderInteraction => filterFeedbackGroup(oi.choices.filter(_.feedbackInline.isDefined).map(_.feedbackInline.get))
            .foreach(fi => feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi))))
        }
      })
      filterFeedbacks(sd.qtiItem.itemBody.feedbackBlocks).foreach(fi =>
        feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi)))
      )
      JsObject(Seq(
        "feedbackContents" -> JsObject(feedbackContents),
        "correctResponses" -> JsObject(correctResponses)
      ))
    }
  }
}


