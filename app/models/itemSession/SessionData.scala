package models.itemSession

import models._
import play.api.libs.json.{Json, JsValue, Writes}
import com.codahale.jerkson.{Json => Jerkson}
import qti.models._
import models.StringItemResponse
import qti.models.QtiItem.Correctness

case class SessionData(correctResponses: Seq[ItemResponse] = Seq(), feedbackContents: Map[String, String] = Map())

object SessionData {

  implicit object Writes extends Writes[SessionData] {

    def writes(sd: SessionData): JsValue = {
      val s = Jerkson.generate(sd)
      Json.parse(s)
    }
  }

  def apply(qti: QtiItem, session: ItemSession): SessionData = {

    val _correctResponses = getCorrectResponses(qti)

    def showCorrectResponses = session.isFinished && session.settings.highlightCorrectResponse
    def showFeedback = session.settings.showFeedback

    def isResponseCorrect(ir: ItemResponse, rd: ResponseDeclaration) = ir match {
      case StringItemResponse(_, value, _) => rd.isCorrect(value)
      case ArrayItemResponse(_, value, _) => rd.isCorrect(value.mkString(","))
    }

    /**
     * 1. find the interaction
     * 2. find the choice
     * 3. find feedback
     * -> if custom feedback -> return feedback id + text
     * -> if default
     * -> is correct?
     * if yes return feedback.id + correct default
     * if no return feedback.id + incorrect default
     * @param userResponse
     * @return
     */
    def createFeedback(userResponse: ItemResponse): Option[(String, String)] = {

      qti.getFeedback(userResponse.id, userResponse.value) match {
        case Some(fb) => {
          if (fb.defaultFeedback)
            None
          else
            Some((fb.csFeedbackId, fb.content))
        }
        case None => None
      }
    }

    def getFeedbackContents: Map[String, String] = {
      if (showFeedback) {

        val feedbackTuples: Seq[(String, String)] = session.responses.map(createFeedback).flatten
        feedbackTuples.toMap[String, String]

      } else {
        Map()
      }
    }

    val feedback: Map[String, String] = getFeedbackContents

    SessionData(
      if (showCorrectResponses) _correctResponses else Seq(),
      feedback
    )
  }

  private def getCorrectResponses(qti: QtiItem): Seq[ItemResponse] = declarationsToItemResponse(qti.responseDeclarations)


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


