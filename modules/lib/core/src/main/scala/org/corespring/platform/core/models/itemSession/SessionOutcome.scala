package org.corespring.platform.core.models.itemSession

import org.corespring.qti.models.{ ResponseDeclaration, QtiItem }
import scalaz._
import Scalaz._
import org.corespring.common.log.ClassLogging
import org.corespring.qti.models.responses.Response
import org.corespring.qti.models.responses.processing.ResponseProcessing
import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.corespring.platform.core.models.error.CorespringInternalError
import com.scalapeno.rhinos.EcmaErrorWithSource

case class IdentifierOutcome(score: Double, isCorrect: Boolean, isComplete: Boolean)

object IdentifierOutcome {
  implicit val identifierOutcomeReads = (
    (__ \ "score").read[Double] and
    (__ \ "isCorrect").read[Boolean] and
    (__ \ "isComplete").read[Boolean])(IdentifierOutcome.apply _)

  implicit val identifierOutcomeWrites = new Writes[IdentifierOutcome] {
    def writes(o: IdentifierOutcome): JsValue = Json.obj(
      "score" -> JsNumber(o.score),
      "isCorrect" -> JsBoolean(o.isCorrect),
      "isComplete" -> JsBoolean(o.isComplete))
  }
}
case class SessionOutcome(score: Double,
  isCorrect: Boolean,
  isComplete: Boolean,
  identifierOutcomes: Map[String, IdentifierOutcome] = Map(),
  script: Option[String] = None)

object SessionOutcome extends ClassLogging {

  def processSessionOutcome(itemSession: ItemSession, qtiItem: QtiItem, debugMode: Boolean): Validation[CorespringInternalError, SessionOutcome] = {
    qtiItem.responseProcessing match {
      case Some(processing) => responseProcessingScoring(itemSession, qtiItem, processing)(debugMode)
      case _ => defaultScoring(itemSession, qtiItem)
    }
  }

  implicit val sessionOutcomeReads = (
    (__ \ "score").read[Double] and
    (__ \ "isCorrect").read[Boolean] and
    (__ \ "isComplete").read[Boolean] and
    (__ \ "identifierOutcomes").read[Map[String, IdentifierOutcome]])((score, isCorrect, isComplete, identifierOutcomes) => SessionOutcome.apply(score, isCorrect, isComplete, identifierOutcomes))

  def fromJsObject(json: JsValue, responseDeclarations: Seq[ResponseDeclaration] = Seq()): JsResult[SessionOutcome] = {

    /**
     * Outcomes should only be serialized if:
     *   1. There is a default correctness in the response declaration OR
     *   2. There is a value for the identifier in the provided JSON
     */
    def useOutcome(responseDeclaration: ResponseDeclaration) =
      responseDeclaration.hasDefaultCorrectResponse ||
        !((json \ "identifierOutcomes" \ responseDeclaration.identifier).isInstanceOf[JsUndefined])

    def computeIdentifierOutcomes(json: JsValue): JsResult[Map[String, IdentifierOutcome]] = {
      responseDeclarations.filter(useOutcome(_)).map(d =>
        d.identifier -> Json.fromJson[IdentifierOutcome]((json \ "identifierOutcomes" \ d.identifier)))
        .foldLeft[JsResult[Map[String, IdentifierOutcome]]](JsSuccess(Map()))((result, input) => {
          result.fold[JsResult[Map[String, IdentifierOutcome]]](
            JsError(_),
            identifierOutcomes => input._2.fold[JsResult[Map[String, IdentifierOutcome]]](
              e => JsError(e),
              io => JsSuccess(identifierOutcomes + (input._1 -> io))))
        })
    }
    (
      (__ \ "score").read[Double] and
      (__ \ "isCorrect").read[Boolean] and
      (__ \ "isComplete").read[Boolean] and
      Reads.apply(computeIdentifierOutcomes) and
      (__ \ "script").readNullable[String])(SessionOutcome.apply _).reads(json)
  }

  private def responseProcessingScoring(
    itemSession: ItemSession,
    qtiItem: QtiItem, responseProcessing: ResponseProcessing)(implicit debugMode: Boolean): Validation[CorespringInternalError, SessionOutcome] = {

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
                    "isComplete" -> JsBoolean(outcome.isComplete)))
              }
            })

            try {
              val response = responseProcessing.process(
                Some(Map("itemSession" -> Json.toJson(itemSession)) ++ identifierDefaults), Some(itemSession.responses))

              response match {
                case Some((script: String, jsObject: JsObject)) => {
                  val result = Writes.writes(defaultOutcome).deepMerge(jsObject)
                  ResponseProcessingOutputValidator(result, qtiItem).map(so => if (debugMode) so.copy(script = Some(script)) else so)
                }
                case Some((script: String, jsNumber: JsNumber)) => {
                  val result = Writes.writes(defaultOutcome).deepMerge(Json.obj("score" -> jsNumber))
                  ResponseProcessingOutputValidator(result, qtiItem).map(so => if (debugMode) so.copy(script = Some(script)) else so)
                }
                case _ => Failure(CorespringInternalError(s"""Response processing for item did not return a JsObject"""))
              }
            } catch {
              case e: EcmaErrorWithSource => {
                println(e.source)
                throw new RuntimeException(e);
              }
            }

          }
          case _ => Failure(CorespringInternalError(s"Default scoring failed"))
        }
      }
      case f: Failure[CorespringInternalError, SessionOutcome] => Failure(CorespringInternalError(s"Default scoring failed:\n ${f.e.message}"))
    }

  }

  private def defaultScoring(session: ItemSession, qtiItem: QtiItem): Validation[CorespringInternalError, SessionOutcome] = {
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
      }).success[CorespringInternalError]
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
          "score" -> JsNumber(if (outcome.score.isNaN()) 0.0 else outcome.score),
          "isCorrect" -> JsBoolean(outcome.isCorrect),
          "isComplete" -> JsBoolean(outcome.isComplete)) ++ Seq(
            "identifierOutcomes" -> JsObject((outcome.identifierOutcomes.map {
              case (identifier, identifierOutcome) => {
                identifier -> Json.toJson(identifierOutcome)
              }
            }).toSeq)) ++ outcome.script.map(s => Seq("script" -> JsString(s))).getOrElse(Seq()))
    }
  }

}
