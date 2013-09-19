package org.corespring.platform.core.models.itemSession

import org.corespring.qti.models.{ ResponseDeclaration, QtiItem }
import scalaz._
import Scalaz._
import org.corespring.common.log.ClassLogging
import org.corespring.qti.models.responses.Response
import org.corespring.qti.models.responses.processing.ResponseProcessing
import play.api.libs.json._
import org.corespring.platform.core.models.error.InternalError

case class SessionOutcome(
  score: Double, isCorrect: Boolean, isComplete: Boolean, identifierOutcomes: Option[Map[String, SessionOutcome]] = None)

object SessionOutcome extends ClassLogging {

  def processSessionOutcome(itemSession: ItemSession, qtiItem: QtiItem): Validation[InternalError, SessionOutcome] = {
    qtiItem.responseProcessing match {
      case Some(processing) => responseProcessingScoring(itemSession, qtiItem, processing)
      case _ => defaultScoring(itemSession, qtiItem)
    }
  }

  def fromJsObject(jsObject: JsObject, responseDeclarations: Option[Seq[ResponseDeclaration]] = None): SessionOutcome = {
    new SessionOutcome(
      score = (jsObject \ "score").asInstanceOf[JsNumber].value.toDouble,
      isCorrect = (jsObject \ "isCorrect").asInstanceOf[JsBoolean].value,
      isComplete = (jsObject \ "isComplete").asInstanceOf[JsBoolean].value,
      identifierOutcomes = responseDeclarations match {
        case Some(declarations: Seq[ResponseDeclaration]) =>
          Some(declarations.map(_.identifier).map(id => id -> fromJsObject((jsObject \ id).asInstanceOf[JsObject])).toMap)
        case None => None
      })
  }

  private def responseProcessingScoring(
    itemSession: ItemSession,
    qtiItem: QtiItem, responseProcessing: ResponseProcessing): Validation[InternalError, SessionOutcome] = {

    defaultScoring(itemSession, qtiItem) match {
      case s: Success[_, SessionOutcome] => {
        s.getOrElse(null) match {
          case defaultOutcome: SessionOutcome => {
            val identifierDefaults = defaultOutcome.identifierOutcomes match {
              case Some(outcomes) => outcomes.map({
                case (identifier, outcome) => {
                  identifier -> Json.obj(
                    "outcome" -> Json.obj(
                      "score" -> JsNumber(outcome.score),
                      "isCorrect" -> JsBoolean(outcome.isCorrect),
                      "isComplete" -> JsBoolean(outcome.isComplete)
                    )
                  )
                }
              })
              case None => Map.empty
            }

            val response = responseProcessing.process(
              Some(Map("itemSession" -> Json.toJson(itemSession)) ++ identifierDefaults), Some(itemSession.responses))
            ResponseProcessingOutputValidator(response, qtiItem)
          }
          case _ => Failure(InternalError(s"Default scoring failed"))
        }
      }
      case f: Failure[InternalError, SessionOutcome] => Failure(InternalError(s"Default scoring failed:\n ${f.e.message}"))
    }

  }

  private def defaultScoring(session: ItemSession, qtiItem: QtiItem): Validation[InternalError, SessionOutcome] = {
    session.responses = Score.scoreResponses(session.responses, qtiItem)
    val maxScore = Score.getMaxScore(qtiItem)
    val score = totalScore(session.responses, maxScore)

    SessionOutcome(
      score = score,
      isCorrect = score == 1,
      isComplete = session.isFinished || isMaxAttemptsExceeded(session) || score == 1,
      identifierOutcomes = (session.responses.length > 1) match {
        case true => {
          Some(
            session.responses.map(response => {
              response.outcome match {
                case Some(outcome) => {
                  val score = outcome.score.toDouble
                  Some(response.id ->
                    SessionOutcome(score, score == 1, session.isFinished || isMaxAttemptsExceeded(session) || score == 1, None))
                }
                case _ => None
              }
            }).flatten.toMap
          )
        }
        case _ => {
          None
        }
      }).success[InternalError]
  }

  private def isMaxAttemptsExceeded(session: ItemSession): Boolean = {
    val max = session.settings.maxNoOfAttempts
    max != 0 && session.attempts >= max
  }

  private def totalScore(responses: Seq[Response], scoreableResponses: Int): Double = {
    val sum = responses.foldRight[Double](0)((r, acc) => {
      acc + r.outcome.map(_.score.toDouble).getOrElse(0.toDouble)
    })
    val average = sum / scoreableResponses
    return average
  }

  implicit object Writes extends Writes[SessionOutcome] {
    def writes(outcome: SessionOutcome): JsValue = {
      JsObject(
        Seq(
          "score" -> JsNumber(outcome.score),
          "isCorrect" -> JsBoolean(outcome.isCorrect),
          "isComplete" -> JsBoolean(outcome.isComplete)) ++ (
            outcome.identifierOutcomes match {
              case Some(outcomes) => outcomes.map {
                case (identifier, sessionOutcome) => {
                  identifier -> writes(sessionOutcome)
                }
              }
              case _ => Seq.empty
            }))
    }
  }

}
