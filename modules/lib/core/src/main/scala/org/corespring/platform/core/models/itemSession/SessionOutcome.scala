package org.corespring.platform.core.models.itemSession

import play.api.libs.json.{JsNumber, JsObject, JsValue, Writes}
import org.corespring.qti.models.QtiItem
import scalaz._
import Scalaz._
import org.corespring.platform.core.models.error.InternalError
import org.corespring.qti.models.responses.Response

case class SessionOutcome(score: Double, isCorrect: Boolean, isComplete: Boolean)
object SessionOutcome{
  def processSessionOutcome(session:ItemSession, qtiItem:QtiItem):Validation[InternalError,SessionOutcome] = {
    defaultScoring(session,qtiItem)
  }
  private def defaultScoring(session:ItemSession, qtiItem:QtiItem):Validation[InternalError,SessionOutcome] = {
    session.responses = Score.scoreResponses(session.responses, qtiItem)
    val maxScore = Score.getMaxScore(qtiItem)
    val score = totalScore(session.responses,maxScore)
    SessionOutcome(score,score==1, session.isFinished || isMaxAttemptsExceeded(session) || score == 1).success[InternalError]
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
  implicit object SOReads extends Writes[SessionOutcome]{
    def writes(outcome: SessionOutcome):JsValue = {
      JsObject(Seq(
        "score" -> JsNumber(outcome.score)
      ))
    }
  }
}
