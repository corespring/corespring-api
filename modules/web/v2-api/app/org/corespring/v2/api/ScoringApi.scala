package org.corespring.v2.api

import org.corespring.models.auth.ApiClient
import org.corespring.models.item.PlayerDefinition
import org.corespring.v2.api.services.{ OrgScoringService, ScoreResult, ScoreService }
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent._
import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

case class ScoringApiExecutionContext(context: ExecutionContext, contextForScoring: ExecutionContext)

class ScoringApi(
  sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition],
  scoreService: ScoreService,
  apiContext: ScoringApiExecutionContext,
  orgScoringService: OrgScoringService,
  val identifyFn: RequestHeader => Validation[V2Error, (OrgAndOpts, ApiClient)],
  val orgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {

  override implicit def ec: ExecutionContext = apiContext.context

  private lazy val logger = Logger(classOf[ItemSessionApi])

  override def getOrgAndOptionsFn: (RequestHeader) => Validation[V2Error, OrgAndOpts] = r => {
    identifyFn(r).map(_._1)
  }

  /**
   * Returns the score for the given session.
   * If the session doesn't contain a 'components' object, an error will be returned.
   *
   * @param sessionId
   * @return
   */
  def loadScore(sessionId: String): Action[AnyContent] = Action.async { implicit request =>

    logger.debug(s"function=loadScore sessionId=$sessionId")

    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        score <- getSingleScore(sessionId, identity)
      } yield score

      validationToResult[JsValue](j => Ok(j))(out)
    }
  }

  private def getSessionIdsFromRequest(r: Request[AnyContent]): Validation[V2Error, Seq[String]] = {
    r.body.asJson.flatMap { jsonBody =>
      (jsonBody \ "sessionIds").asOpt[JsArray]
        .map(_.as[Seq[String]])
    }.toSuccess(missingSessionIds())
  }

  def loadMultipleScoresTwo(): Action[AnyContent] = Action.async { implicit request =>

    val out = for {
      identity <- getOrgAndOptions(request)
      ids <- getSessionIdsFromRequest(request)
    } yield orgScoringService.scoreMultipleSessions(identity)(ids)

    out match {
      case Failure(e) => Future.successful(Status(e.statusCode)(e.json))
      case Success(f) => f.map { results =>
        Ok(Json.toJson(results.map(resultToJson)))
      }
    }
  }

  private def resultToJson(item: ScoreResult): JsValue = item match {
    case ScoreResult(id, Success(score)) => Json.obj("sessionId" -> id, "result" -> score)
    case ScoreResult(id, Failure(e)) => Json.obj("sessionId" -> id, "error" -> e.json)
  }

  /**
   * Loading multiple scores
   * 1. Load all sessions
   * 2. Load all items referenced in the sessions
   * 3. In parallel calculate scores for all pairs of session/item
   *
   * @return a list of json objects that map sessionIds to the result
   * of the scoring.
   *
   * [
   * {sessionId: "1234", "result": {"score": 1},
   * {sessionId: "1234", "error": {"message": "Error scoring"}
   * ]
   */
  def loadMultipleScores(): Action[AnyContent] = Action.async { implicit request =>

    logger.debug(s"function=loadMultipleScores")

    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        ids <- getSessionIdsFromRequest(request)
        items <- Success(sessionAuth.loadForScoringMultiple(ids)(identity))
        scores <- Success(calcScores(items))
      } yield scores

      validationToResult[JsValue](j => Ok(j))(out)
    }
  }

  type ScoreItem = (String, Validation[V2Error, (JsValue, PlayerDefinition)])

  private def calcScores(items: Seq[ScoreItem]) = {

    def calcScore(item: ScoreItem): ScoreResult = item match {
      case (id: String, Success(sessionAndPlayerDef)) => {
        val out = for {
          score <- getScore(id, sessionAndPlayerDef._1, sessionAndPlayerDef._2)
        } yield score
        ScoreResult(id, out)
      }
      case (id: String, Failure(e)) => ScoreResult(id, Failure(e))
    }

    JsArray(
      Await.result(
        Future.sequence(
          items.map { item =>
            Future {
              resultToJson(calcScore(item))
            }(apiContext.contextForScoring)
          }), 20.seconds))
  }

  private def getSingleScore(sessionId: String, identity: OrgAndOpts): Validation[V2Error, JsValue] = {
    for {
      sessionAndPlayerDef <- sessionAuth.loadForScoring(sessionId)(identity)
      score <- getScore(sessionId, sessionAndPlayerDef._1, sessionAndPlayerDef._2)
    } yield score
  }

  private def getScore(sessionId: String, session: JsValue, playerDef: PlayerDefinition): Validation[V2Error, JsValue] = {
    for {
      components <- getComponents(session).toSuccess(sessionDoesNotContainResponses(sessionId))
      score <- scoreService.score(playerDef, components)
    } yield score
  }

  private def getComponents(session: JsValue): Option[JsValue] = {
    (session \ "components").asOpt[JsObject]
  }
}

