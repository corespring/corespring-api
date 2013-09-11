package org.corespring.platform.core.models.itemSession

import org.corespring.qti.models.QtiItem
import scalaz._
import Scalaz._
import org.corespring.platform.core.models.error.InternalError
import org.corespring.qti.models.responses.Response
import org.corespring.qti.models.responses.processing.ResponseProcessing
import play.api.libs.json._

case class SessionOutcome(score: Double, isCorrect: Boolean, isComplete: Boolean)

object SessionOutcome {

  def processSessionOutcome(itemSession: ItemSession, qtiItem: QtiItem): Validation[InternalError,SessionOutcome] =
    qtiItem.responseProcessing match {
      case Some(processing) => responseProcessingScoring(itemSession, qtiItem, processing)
      case _ => defaultScoring(itemSession, qtiItem)
    }

  def apply(score: Double, itemSession: ItemSession) =
    new SessionOutcome(
      score = score,
      isCorrect = score==1,
      isComplete = isMaxAttemptsExceeded(itemSession) || score == 1
    )

  private def responseProcessingScoring(itemSession: ItemSession, qtiItem: QtiItem, responseProcessing: ResponseProcessing): Validation[InternalError, SessionOutcome] = {
    responseProcessing.process(Map("itemSession" -> Json.toJson(itemSession))) match {
      case Some(score) => score match {
        case double: Double => Success(SessionOutcome(double, itemSession))
        case _ => Failure(InternalError(s"""Response processing for item ${itemSession.itemId} did not return a Double"""))
      }
      case _ => Failure(InternalError(s"""Response processing for item ${itemSession.itemId} did not return a score"""))
    }
  }

  private def defaultScoring(session: ItemSession, qtiItem: QtiItem): Validation[InternalError,SessionOutcome] = {
    session.responses = Score.scoreResponses(session.responses, qtiItem)
    val maxScore = Score.getMaxScore(qtiItem)
    val score = totalScore(session.responses,maxScore)

    SessionOutcome(score, session).success[InternalError]
  }

  private def isMaxAttemptsExceeded(session: ItemSession): Boolean = {
    val max = session.settings.maxNoOfAttempts
    max != 0 && session.attempts >= max
  }

  private def totalScore(responses: Seq[Response], scoreableResponses:Int): Double = {
    val sum = responses.foldRight[Double](0)((r,acc) => {
      acc + r.outcome.map(_.score.toDouble).getOrElse(0.toDouble)
    })
    val average = sum / scoreableResponses
    return average
  }

  implicit object SOReads extends Writes[SessionOutcome] {
    def writes(outcome: SessionOutcome):JsValue = {
      JsObject(Seq(
        "score" -> JsNumber(outcome.score)
      ))
    }
  }

}
