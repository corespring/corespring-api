package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.{ Json, JsValue }

import scalaz.Validation

trait ScoreService {

  /**
   * get a score for a given item
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
  def score(item: Item, answers: JsValue): Validation[V2Error, JsValue]
}

import scalaz.Scalaz._

class BasicScoreService(outcomeProcessor: OutcomeProcessor, scoreProcessor: ScoreProcessor) extends ScoreService {

  protected lazy val logger = V2LoggerFactory.getLogger(this.getClass.getSimpleName)

  def noPlayerDefinition(id: VersionedId[ObjectId]): V2Error = generalError(s"This item ($id) has no player definition, unable to calculate a score")

  override def score(item: Item, answers: JsValue): Validation[V2Error, JsValue] = {

    logger.debug(s"function=score itemId=${item.id}")
    logger.trace(s"function=score answers=${Json.stringify(answers)}")

    //TODO: Should we be doing some uid validation here, to make sure that they aren't sending unused uids.
    //TODO: Currently they'll just be ignored

    item.playerDefinition.map { pd =>
      val itemJson = Json.toJson(pd)
      /** Because we are only getting the score we don't care about feedback */
      val blankSettings = Json.obj()
      val componentAnswers = Json.obj("components" -> answers)
      val outcome = outcomeProcessor.createOutcome(itemJson, componentAnswers, blankSettings)
      scoreProcessor.score(itemJson, answers, outcome)
    }.toSuccess(noPlayerDefinition(item.id))
  }

}
