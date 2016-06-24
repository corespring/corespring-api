package org.corespring.v2.api

import org.corespring.v2.api.services.{ OrgScoringService, ScoreResult }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent._
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

case class ScoringApiExecutionContext(context: ExecutionContext, contextForScoring: ExecutionContext)

class ScoringApi(
  actions : org.corespring.v2.actions.V2Actions,
  apiContext: ScoringApiExecutionContext,
  orgScoringService: OrgScoringService)
  extends V2Api {

  override implicit def ec: ExecutionContext = apiContext.context

  private lazy val logger = Logger(classOf[ItemSessionApi])

  /**
   * Returns the score for the given session.
   * If the session doesn't contain a 'components' object, an error will be returned.
   *
   * @param sessionId
   * @return
   */
  def loadScore(sessionId: String): Action[AnyContent] = actions.Org.async { implicit request =>

    logger.debug(s"function=loadScore sessionId=$sessionId")

    orgScoringService.scoreSession(request.orgAndOpts)(sessionId).map{ r =>
      r.result.toSimpleResult()
    }
  }

  private def getSessionIdsFromRequest(r: Request[AnyContent]): Validation[V2Error, Seq[String]] = {
    r.body.asJson.flatMap { jsonBody =>
      (jsonBody \ "sessionIds").asOpt[JsArray]
        .map(_.as[Seq[String]])
    }.toSuccess(missingSessionIds())
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
  def loadMultipleScores(): Action[AnyContent] = actions.Org.async { implicit request =>

    val out = for {
      ids <- getSessionIdsFromRequest(request)
    } yield orgScoringService.scoreMultipleSessions(request.orgAndOpts)(ids)

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

}

