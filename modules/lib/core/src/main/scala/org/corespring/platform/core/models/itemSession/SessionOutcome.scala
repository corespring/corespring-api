package org.corespring.platform.core.models.itemSession

import org.corespring.qti.models.{ResponseDeclaration, QtiItem}
import scalaz._
import Scalaz._
import org.corespring.common.log.ClassLogging
import org.corespring.platform.core.models.error.InternalError
import org.corespring.qti.models.responses.Response
import org.corespring.qti.models.responses.processing.ResponseProcessing
import play.api.libs.json._

case class SessionOutcome(score: Double, isCorrect: Boolean, isComplete: Boolean)

object SessionOutcome extends ClassLogging {

  def processSessionOutcome(itemSession: ItemSession, qtiItem: QtiItem): Validation[InternalError,SessionOutcome] = {
    qtiItem.responseProcessing match {
      case Some(processing) => responseProcessingScoring(itemSession, qtiItem, processing)
      case _ => defaultScoring(itemSession, qtiItem)
    }
  }

  def apply(score: Double, itemSession: ItemSession) =
    new SessionOutcome(
      score = score,
      isCorrect = score==1,
      isComplete = isMaxAttemptsExceeded(itemSession) || score == 1
    )

  private def responseProcessingScoring(itemSession: ItemSession, qtiItem: QtiItem, responseProcessing: ResponseProcessing): Validation[InternalError, SessionOutcome] = {
    responseProcessing.process(Map("itemSession" -> Json.toJson(itemSession))) match {
      case Some(jsValue: JsValue) => {
        checkJsResponse(jsValue, qtiItem.responseDeclarations) match {
          case Some(internalError) => Failure(internalError)
          case _ => {
            Success(new SessionOutcome(0, true, true))
          }
        }
      }
      case _ => Failure(InternalError(s"""Response processing for item ${itemSession.itemId} did not return a JsObject"""))
    }
  }

  private def checkJsResponse(jsValue: JsValue, responseDeclarations: Seq[ResponseDeclaration]): Option[InternalError] = {
    validateJsResponse(jsValue) match {
      case Some(internalError) => Some(internalError)
      case _ => {
        val identifiers = responseDeclarations.map(_.identifier)
        identifiers.find(identifier => (jsValue \ identifier).isInstanceOf[JsUndefined]) match {
          case Some(identifier) =>
            Some(InternalError(s"""Response for identifier $identifier is required in JsObject"""))
          case _ => {
            val errors = identifiers.flatMap(identifier => validateJsResponse((jsValue \ identifier), Some(identifier)))
            errors match {
              case errors: Seq[InternalError] if errors.nonEmpty => {
                Some(errors.head)
              }
              case _ => None
            }
          }
        }
      }
    }
  }

  private def validateJsResponse(jsValue: JsValue, identifier: Option[String] = None): Option[InternalError] = {
    val identifierString: String = identifier match {
      case Some(string) => s""" for responseDeclaration identifier $string"""
      case None => ""
    }
    jsValue match {
      case jsObject: JsObject => {
        (jsObject \ "score") match {
          case JsNumber(_) => {
            (jsObject \ "isComplete") match {
              case JsBoolean(_) => {
                (jsObject \ "isCorrect") match {
                  case JsBoolean(_) => None
                  case JsUndefined(_) =>
                    Some(InternalError(s"""isCorrect is required in Javascript response object$identifierString"""))
                  case _ =>
                    Some(InternalError(s"""isCorrect is required to be a JsBoolean object$identifierString"""))
                }
              }
              case JsUndefined(_) =>
                Some(InternalError(s"""isComplete is required in Javascript response object$identifierString"""))
              case _ =>
                Some(InternalError(s"""isComplete is required to be a JsBoolean object$identifierString"""))
            }
          }
          case JsUndefined(_) =>
            Some(InternalError(s"""score is required in Javascript response object$identifierString"""))
          case _ => Some(InternalError(s"""score is required to be a JsNumber object$identifierString"""))
        }
      }
      case _ => Some(InternalError(s"""Response processed by Javascript was not a JsObject$identifierString"""))
    }
  }

  private def defaultScoring(session: ItemSession, qtiItem: QtiItem): Validation[InternalError,SessionOutcome] = {
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

  implicit object SOReads extends Writes[SessionOutcome] {
    def writes(outcome: SessionOutcome):JsValue = {
      JsObject(Seq(
        "score" -> JsNumber(outcome.score)
      ))
    }
  }

}
