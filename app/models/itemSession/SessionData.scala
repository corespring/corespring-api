package models.itemSession

import models._
import play.api.libs.json.Json._
import qti.models._
import play.api.libs.json._
import play.api.libs.json.JsArray
import models.StringItemResponse
import scala.Some
import play.api.libs.json.JsObject
import models.ArrayItemResponse

case class SessionData(correctResponses: Seq[ItemResponse] = Seq(), feedbackContents: Map[String, String] = Map())

object SessionData {

  implicit object Writes extends Writes[SessionData] {
    def writes(sd: SessionData): JsValue = {

      def tupleToJs(t:(String,String)) : (String,JsString) = (t._1,JsString(t._2))

      JsObject(Seq(
        ("correctResponses" -> JsArray(sd.correctResponses.map(toJson(_)))),
        ("feedbackContents" -> JsObject(sd.feedbackContents.toSeq.map(tupleToJs)))
      ))
    }
  }

  def apply(qti: QtiItem, session: ItemSession): SessionData = {

    def showCorrectResponses = session.isFinished && session.settings.highlightCorrectResponse
    def showFeedback = session.settings.showFeedback

    val allCorrectResponses = declarationsToItemResponse(qti.responseDeclarations)

    def createFeedback( idValueIndex : (String, String, Int)): Option[(String, String)] = {
      val (id,value, index) = idValueIndex
      qti.getFeedback(id, value) match {
        case Some(fb) => {

          println("found feedback for: %s, %s, %s".format(id,value,index) )
          println(fb)
          if (fb.defaultFeedback)
            Some(fb.csFeedbackId, getDefaultFeedback(id, value, index))
          else
            Some((fb.csFeedbackId, fb.content))
        }
        case None => None
      }
    }

    def getDefaultFeedback(id:String, value : String, index : Int) : String = {
      if(qti.isValueCorrect(id, value, index))
        qti.defaultCorrect
       else
        qti.defaultIncorrect
    }

    def getFeedbackContents: Map[String, String] = {
      if (showFeedback) {
        val userResponses = session.responses.map(_.getIdValueIndex).flatten
        val correctResponses = if (showCorrectResponses) makeCorrectResponseList else Seq()
        val responsesToGiveFeedbackOn : List[(String,String,Int)] =
          (userResponses.toList ::: correctResponses.toList).distinct

        val feedbackTuples: List[(String, String)] = responsesToGiveFeedbackOn.map(createFeedback).flatten
        feedbackTuples.toMap[String, String]
      } else {
        Map()
      }
    }

    def makeCorrectResponseList : Seq[(String,String,Int)] = {
      val answered = allCorrectResponses.filter( (cr:ItemResponse) => {
        val id = cr.id
        session.responses.exists( _.id == id)
      })
      answered.map(_.getIdValueIndex).flatten
    }


    SessionData(
      if (showCorrectResponses) allCorrectResponses else Seq(),
      getFeedbackContents
    )
  }


  /**
   * convert ResponseDeclaration.CorrectResponse -> ItemResponse
   * @param declarations
   * @return
   */
  private def declarationsToItemResponse(declarations: Seq[ResponseDeclaration]): List[ItemResponse] = {

    def correctResponseToItemResponse(id: String)(cr: CorrectResponse): ItemResponse = cr match {
      case CorrectResponseSingle(value) => StringItemResponse(id, value)
      case CorrectResponseMultiple(value) => ArrayItemResponse(id, value)
      case CorrectResponseAny(value) => ArrayItemResponse(id, value)
      case CorrectResponseOrdered(value) => ArrayItemResponse(id, value)
      case _ => throw new RuntimeException("Unknown CorrectResponseType: " + cr)
    }

    def _declarationsToItemResponses(declarations: Seq[ResponseDeclaration]): List[ItemResponse] = {
      if (declarations.isEmpty) {
        List()
      } else {
        val rd: ResponseDeclaration = declarations.head
        val correctResponseViews = rd.correctResponse.map(correctResponseToItemResponse(rd.identifier))
        correctResponseViews.toList ::: _declarationsToItemResponses(declarations.tail)
      }
    }
    _declarationsToItemResponses(declarations)
  }
}