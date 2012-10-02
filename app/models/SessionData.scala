package models

import play.api.libs.json._
import play.api.libs.json.JsString
import scala.xml._
import models.bleezmo._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import collection.immutable.HashMap
import controllers.Log


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

      def filterFeedbacks(feedbacks:Seq[FeedbackInline],displayCorrectResponse:Boolean = false):Seq[FeedbackInline] = {
        var feedbackGroups:HashMap[String,Seq[FeedbackInline]] = HashMap()
        feedbacks.foreach(fi => if (feedbackGroups.get(fi.responseIdentifier).isDefined){
          feedbackGroups += (fi.responseIdentifier -> (feedbackGroups.get(fi.responseIdentifier).get :+ fi))
        }else{
          feedbackGroups += (fi.responseIdentifier -> Seq(fi))
        })
        feedbackGroups.map(kvpair => filterFeedbackGroup(kvpair._1, kvpair._2,displayCorrectResponse)).flatten.toSeq
      }
      def filterFeedbackGroup(responseIdentifier:String, feedbackGroup:Seq[FeedbackInline], displayCorrectResponse:Boolean = true):Seq[FeedbackInline] = {
        //add feedbackInline to feedbackContents if it was a response or it is the correct answer denoted by responseDeclaration
        val responseGroup = sd.responses.filter(ir => ir.id == responseIdentifier)
        val feedbackContents = feedbackGroup.filter(fi => responseGroup.find(response => {
         // Log.i("comparing response: "+response.toString+"\n\twith feedbackInline: "+fi.toString)
          if (response.value.contains(ItemResponse.Delimiter)){
            response.value.split(ItemResponse.Delimiter).find(_ == fi.identifier).isDefined
          }else response.value == fi.identifier
        }).isDefined || (displayCorrectResponse && sd.qtiItem.responseDeclarations
          .find(_.identifier == fi.responseIdentifier)
          .map(_.isCorrect(fi.identifier)).getOrElse(false)))
        if(feedbackContents.isEmpty){
          feedbackGroup.find(fi => fi.incorrectResponse) match {
            case Some(fi) => Seq(fi)
            case None => Seq()
          }
        }else feedbackContents
      }

      def getFeedbackContent(fi:FeedbackInline) = if(fi.defaultFeedback)
        fi.defaultContent(sd.qtiItem)
      else fi.content

      var feedbackContents:Seq[(String,JsValue)] = Seq()
      sd.qtiItem.itemBody.interactions.foreach(interaction => {
        interaction match {
          case ci:ChoiceInteraction => filterFeedbackGroup(ci.responseIdentifier, ci.choices.map(_.feedbackInline).flatten,true)
            .foreach(fi => feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi))))
          case oi:OrderInteraction => filterFeedbackGroup(oi.responseIdentifier, oi.choices.map(_.feedbackInline).flatten,true)
            .foreach(fi => feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi))))
        }
      })
      filterFeedbacks(sd.qtiItem.itemBody.feedbackBlocks,false).foreach(fi =>
        feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi)))
      )
      JsObject(Seq(
        "feedbackContents" -> JsObject(feedbackContents),
        "correctResponses" -> JsObject(correctResponses)
      ))
    }
  }
}


