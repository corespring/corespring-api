package org.corespring.v2.api.services

import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.models.item.PlayerDefinition
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json.{ JsValue, Json, Writes }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Success, Validation }

trait ScoreService {

  /**
   * get a score for a given item
   *
   * @param item
   * @param answers
   * Should be in the format:
   * {
   *   "uid" : {
   *     "answers" : ???
   *   },
   *   ...
   * }
   * @return either a V2Error or the score
   *         The score will be in the format:
   *         {
   *         summary : {
   *         maxPoints : number,
   *         points : number,
   *         percentage : (0.0 - 100.0)
   *         },
   *         components : {
   *         uid : {
   *         weight : number,
   *         score : number (0.0 - 1.0),
   *         weightedScore : number
   *         },
   *         ....
   *         }
   *         }
   */
  def score(playerDefinition: PlayerDefinition, answers: JsValue): Validation[V2Error, JsValue]
  def scoreMultiple(playerDefinition: PlayerDefinition, answers: Seq[JsValue]): Seq[Future[(JsValue, Validation[V2Error, JsValue])]]
}

case class ScoreServiceExecutionContext(ec: ExecutionContext)

class BasicScoreService(
  outcomeProcessor: OutcomeProcessor,
  scoreProcessor: ScoreProcessor,
  scoreServiceContext: ScoreServiceExecutionContext)(implicit val w: Writes[PlayerDefinition]) extends ScoreService {

  private lazy val logger = Logger(this.getClass)

  override def scoreMultiple(playerDefinition: PlayerDefinition, answers: Seq[JsValue]): Seq[Future[(JsValue, Validation[V2Error, JsValue])]] = {
    answers.map { a =>
      logger.trace(s"function=scoreMultiple, a=${Json.toJsFieldJsValueWrapper(a)}, playerDefinition=$playerDefinition")
      Future { (a -> scoreSession(playerDefinition, a)) }(scoreServiceContext.ec)
    }
  }

  override def score(pd: PlayerDefinition, answers: JsValue): Validation[V2Error, JsValue] = {
    scoreSession(pd, Json.obj("components" -> answers))
  }

  private def scoreSession(pd: PlayerDefinition, session: JsValue): Validation[V2Error, JsValue] = {
    val pdJson = Json.toJson(pd)
    logger.trace(s"function=score session=${Json.stringify(session)}")

    //TODO: Should we be doing some uid validation here, to make sure that they aren't sending unused uids.
    //TODO: Currently they'll just be ignored

    /** Because we are only getting the score we don't care about feedback */
    val blankSettings = Json.obj()
    //TODO: Scoring should be asynchronous
    val outcome = outcomeProcessor.createOutcome(pdJson, session, blankSettings)
    val score = scoreProcessor.score(pdJson, session, outcome)
    logger.trace(s"function=score, score=${Json.prettyPrint(score)}, session=${Json.prettyPrint(session)}, pdJson=${Json.prettyPrint(pdJson)}, outcome=${Json.prettyPrint(outcome)}")
    Success(score)
  }

}
