package org.corespring.v2.api.services

import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.platform.core.models.item.Item
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ Json, JsValue }

import scalaz.Validation

trait ScoreService {

  /**
   * get a score for a given item
   * @param item
   * @param answers
   * @return either a V2Error or the score
   *         The score will be in the format:
   * {
   *   summary : {
   *     maxPoints : number,
   *     points : number,
   *     percentage : (0.0 - 100.0)
   *   },
   *    components : {
   *      uid : {
   *        weight : number,
   *        score : number (0.0 - 1.0),
   *        weightedScore : number
   *      },
   *      ....
   *    }
   * }
   */
  def score(item: Item, answers: JsValue): Validation[V2Error, JsValue]
}

import scalaz.Scalaz._

class BasicScoreService(outcomeProcessor: OutcomeProcessor, scoreProcessor: ScoreProcessor) extends ScoreService {
  override def score(item: Item, answers: JsValue): Validation[V2Error, JsValue] =
    item.playerDefinition.map { pd =>
      val itemJson = Json.toJson(pd)
      val outcome = outcomeProcessor.createOutcome(itemJson, answers, Json.obj())
      scoreProcessor.score(itemJson, answers, outcome)
    }.toSuccess(
      generalError(s"This item (${item.id}) has no player definition"))

}
