package org.corespring.v2.api

import org.corespring.models.auth.ApiClient
import org.corespring.models.item.PlayerDefinition
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent._
import scalaz.Scalaz._
import scalaz.{Failure, Success, Validation}

case class ScoringApiExecutionContext(context: ExecutionContext)

class ScoringApi(
  sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition],
  scoreService: ScoreService,
  apiContext: ScoringApiExecutionContext,
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

    def getComponents(session: JsValue): Option[JsValue] = {
      (session \ "components").asOpt[JsObject]
    }

    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        score <- getSingleScore(sessionId, identity)
      } yield score

      validationToResult[JsValue](j => Ok(j))(out)
    }
  }

  def loadMultipleScores(): Action[AnyContent] = Action.async { implicit request =>

    logger.debug(s"function=loadMultipleScores")

    def getSessionIdsFromRequest() = {
      val jsonBody = request.body.asJson.getOrElse(Json.obj())
      (jsonBody \ "sessionIds").asOpt[JsArray]
        .map(arr => arr.value.map(v => v.as[String]))
    }

    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        ids <- getSessionIdsFromRequest().toSuccess(missingSessionIds())
        scores <- Success(getMultipleScores(ids, identity))
      } yield scores

      validationToResult[JsValue](j => Ok(j))(out)
    }
  }

  private def getMultipleScores(sessionIds: Seq[String], identity: OrgAndOpts) = {
    JsArray(sessionIds.map(sessionId => {
      Json.obj("sessionId" -> sessionId) ++ {
        getSingleScore(sessionId, identity) match {
          case Success(score) => Json.obj("result" -> score)
          case Failure(e) => Json.obj("error" -> e.json)
        }
      }
    }))
  }

  private def getSingleScore(sessionId: String, identity: OrgAndOpts): Validation[V2Error, JsValue] = {
    def getComponents(session: JsValue): Option[JsValue] = {
      (session \ "components").asOpt[JsObject]
    }

    for {
      sessionAndPlayerDef <- sessionAuth.loadForWrite(sessionId)(identity)
      session <- Success(sessionAndPlayerDef._1)
      playerDef <- Success(sessionAndPlayerDef._2)
      components <- getComponents(session).toSuccess(sessionDoesNotContainResponses(sessionId))
      score <- scoreService.score(playerDef, components)
    } yield score
  }
}

