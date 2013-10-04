package org.corespring.platform.core.models.itemSession

import org.corespring.qti.models.{ ResponseDeclaration, QtiItem }
import scalaz._
import Scalaz._
import org.corespring.common.log.ClassLogging
import org.corespring.qti.models.responses.Response
import org.corespring.qti.models.responses.processing.ResponseProcessing
import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.corespring.platform.core.models.error.InternalError
import com.scalapeno.rhinos.EcmaErrorWithSource

case class IdentifierOutcome(score: Double, isCorrect: Boolean, isComplete: Boolean)

object IdentifierOutcome{
  implicit val identifierOutcomeReads = (
    (__ \ "score").read[Double] and
    (__ \ "isCorrect").read[Boolean] and
    (__ \ "isComplete").read[Boolean]
  )(IdentifierOutcome.apply _)

  implicit val identifierOutcomeWrites = new Writes[IdentifierOutcome] {
    def writes(o: IdentifierOutcome): JsValue = Json.obj(
      "score" -> JsNumber(o.score),
      "isCorrect" -> JsBoolean(o.isCorrect),
      "isComplete" -> JsBoolean(o.isComplete)
    )
  }
}
case class SessionOutcome(
  score: Double, isCorrect: Boolean, isComplete: Boolean, identifierOutcomes: Map[String, IdentifierOutcome] = Map())

object SessionOutcome extends ClassLogging {

  def processSessionOutcome(itemSession: ItemSession, qtiItem: QtiItem): Validation[InternalError, SessionOutcome] = {
    qtiItem.responseProcessing match {
      case Some(processing) => responseProcessingScoring(itemSession, qtiItem, processing)
      case _ => defaultScoring(itemSession, qtiItem)
    }
  }

  implicit val sessionOutcomeReads = (
    (__ \ "score").read[Double] and
    (__ \ "isCorrect").read[Boolean] and
    (__ \ "isComplete").read[Boolean] and
    (__ \ "identifierOutcomes").read[Map[String,IdentifierOutcome]]
  )(SessionOutcome.apply _)

  def fromJsObject(json:JsValue, responseDeclarations: Seq[ResponseDeclaration] = Seq()): JsResult[SessionOutcome] = {
      def computeIdentifierOutcomes(json:JsValue):JsResult[Map[String,IdentifierOutcome]]= {
        responseDeclarations.map(d => d.identifier -> Json.fromJson[IdentifierOutcome]((json \ d.identifier)))
          .foldLeft[JsResult[Map[String,IdentifierOutcome]]](JsSuccess(Map()))((result,input) => {
            result.fold(JsError(_),identifierOutcomes => input._2.fold(JsError(_),io => JsSuccess(identifierOutcomes + (input._1 ->  io))))
          })
      }
      (
        (__ \ "score").read[Double] and
        (__ \ "isCorrect").read[Boolean] and
        (__ \ "isComplete").read[Boolean] and
        Reads.apply(computeIdentifierOutcomes)
      )(SessionOutcome.apply _).reads(json)
  }

  private def responseProcessingScoring(
    itemSession: ItemSession,
    qtiItem: QtiItem, responseProcessing: ResponseProcessing): Validation[InternalError, SessionOutcome] = {

    defaultScoring(itemSession, qtiItem) match {
      case s: Success[_, SessionOutcome] => {
        s.getOrElse(null) match {
          case defaultOutcome: SessionOutcome => {
            val identifierDefaults = defaultOutcome.identifierOutcomes.map({
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

            try {
              val response = responseProcessing.process(
                Some(Map("itemSession" -> Json.toJson(itemSession)) ++ identifierDefaults), Some(itemSession.responses))

              response match {
                case Some(jsObject: JsObject) => {
                  val result = Writes.writes(defaultOutcome).deepMerge(jsObject)
                  ResponseProcessingOutputValidator(result, qtiItem)
                }
                case Some(jsNumber: JsNumber) => {
                  val result = Writes.writes(defaultOutcome).deepMerge(Json.obj("score" -> jsNumber))
                  ResponseProcessingOutputValidator(result, qtiItem)
                }
                case _ => Failure(InternalError(s"""Response processing for item did not return a JsObject"""))
              }
            } catch {
              case e: EcmaErrorWithSource => {
                println(e.source)
                throw new RuntimeException(e);
              }
            }

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
      identifierOutcomes = session.responses.nonEmpty match {
        case true => {
            session.responses.map(response => {
              response.outcome match {
                case Some(outcome) => {
                  val score = outcome.score.toDouble
                  Some(response.id ->
                    IdentifierOutcome(score, score == 1, session.isFinished || isMaxAttemptsExceeded(session) || score == 1))
                }
                case _ => None
              }
            }).flatten.toMap
        }
        case _ => Map()
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
    def writes(outcome: SessionOutcome): JsObject = {
      JsObject(
        Seq(
          "score" -> JsNumber(outcome.score),
          "isCorrect" -> JsBoolean(outcome.isCorrect),
          "isComplete" -> JsBoolean(outcome.isComplete)) ++ (
            outcome.identifierOutcomes.map {
              case (identifier, identifierOutcome) => {
                identifier -> Json.toJson(identifierOutcome)
              }
            }))
    }
  }

}
