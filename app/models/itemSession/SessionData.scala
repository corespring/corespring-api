package models.itemSession

import play.api.libs.json.Json._
import qti.models._
import interactions.SelectTextInteraction
import play.api.libs.json._
import play.api.libs.json.JsArray
import scala.Some
import play.api.libs.json.JsObject

case class SessionData(correctResponses: Seq[ItemResponse] = Seq(), feedbackContents: Map[String, String] = Map())

object SessionData {

  type IdValueIndex = (String,String,Int)

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

    def showCorrectResponses = session.settings.highlightCorrectResponse
    def showFeedback = session.settings.showFeedback

    //we need to get the correct responses from SelectTextInteraction manually,
    // because there are no response declaration for that interaction. the correct response is within the interaction
    val selectTextInteractionCorrectResponses:Seq[ItemResponse] = qti.itemBody.interactions.filter(i => i.isInstanceOf[SelectTextInteraction]).
      map(_.asInstanceOf[SelectTextInteraction]).
      filter(_.correctResponse.isDefined).
      map(sti => ArrayItemResponse(sti.responseIdentifier,sti.correctResponse.get.value))



    val allCorrectResponses:List[ItemResponse] = declarationsToItemResponse(qti.responseDeclarations) ++ selectTextInteractionCorrectResponses


    def createFeedback( idValueIndex : (String, String, Int)): Option[(String, String)] = {
      val (id,value, index) = idValueIndex
      val feedback = qti.getFeedback(id,value)
      feedback match {
        case Some(fb) => {
          if (!showFeedback)
            None
          else if (fb.defaultFeedback)
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
        feedbackTuples.toMap[String, String] ++ buildOutcomeFeedbackContents
      } else {
        Map()
      }
    }
    def buildOutcomeFeedbackContents:Map[String,String] = {
      var outcomeFeedback:Map[String,String] = Map();
      val responses = session.responses.filter(_.outcome.isDefined);
      for (response <- responses){
        outcomeFeedback = outcomeFeedback ++ response.outcome.get.getOutcomeBasedFeedbackContents(qti,response.id)
      }
      outcomeFeedback
    }
    def makeCorrectResponseList : Seq[(String,String,Int)] = {

      def getIdValueIndexIfApplicable(response:ItemResponse) : Seq[IdValueIndex] = {
        if(qti.isCorrectResponseApplicable(response.id))
          response.getIdValueIndex
        else
         Seq()
      }

      val answered = allCorrectResponses.filter( (cr) => session.responses.exists( _.id == cr.id) )
      answered.map(getIdValueIndexIfApplicable(_)).flatten
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
      case CorrectResponseEquation(value,_,_,_) => StringItemResponse(id,value)
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