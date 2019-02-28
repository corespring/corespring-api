package org.corespring.container.client.controllers.resources

import org.corespring.container.client.controllers.resources.Session.Errors
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks._
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.processing.PlayerItemPreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.container.logging.ContainerLogger
import play.api.libs.json._
import play.api.mvc.{ Action, Controller, SimpleResult }

import scala.concurrent.{ ExecutionContext, Future }

object Session {
  object Errors {
    val cantSaveWhenComplete = "secure mode: can't save when session is complete"
    val cantResetSecureSession = "secure mode: can't reset secure session"
    val cantReopenSecureSession = "secure mode: can't reopen secure session"
  }
}

//case class to enable auto wiring
case class SessionExecutionContext(default: ExecutionContext, heavyLoad: ExecutionContext)

class Session(
  val outcomeProcessor: OutcomeProcessor,
  val itemPreProcessor: PlayerItemPreProcessor,
  val scoreProcessor: ScoreProcessor,
  val hooks: SessionHooks,
  val sessionContext: SessionExecutionContext)
  extends Controller {

  val logger = ContainerLogger.getLogger("Session")
  val scoringLogger = ContainerLogger.getLogger("Session.scoring")
  val updatesLogger = ContainerLogger.getLogger("Session.updates")
  val loadLogger = ContainerLogger.getLogger("Session.load")

  private def logScore(caller: String, score: JsValue, answers: JsValue, item: JsValue) = {
    if (scoringLogger.isTraceEnabled) {
      //log item too
      scoringLogger.trace(s"[$caller] score: ${Json.stringify(score)} answers: ${Json.stringify(answers)} item: ${Json.stringify(item)}")
    } else {
      //otherwise log score and session only
      scoringLogger.debug(s"[$caller] score: ${Json.stringify(score)} answers: ${Json.stringify(answers)}")
    }
  }

  implicit val ec: ExecutionContext = sessionContext.default

  implicit def toResult(m: StatusMessage): SimpleResult = play.api.mvc.Results.Status(m._1)(Json.obj("error" -> m._2))

  private def basicHandler[A](success: (A => SimpleResult))(
    e: Either[StatusMessage, A]): SimpleResult = e match {
    case Left(err) => err
    case Right(json) => success(json)
  }

  def load(id: String) = Action.async { implicit request =>
    hooks.load(id).map(basicHandler { ss =>
      loadLogger.trace(s"load: ${Json.stringify(ss)}")
      Ok(ss)
    })
  }

  def resetSession(id: String) = Action.async { implicit request =>
    hooks.save(id).map(basicHandler { ss =>
      if (ss.isSecure)
        BadRequest(Json.obj("error" -> JsString(Errors.cantResetSecureSession)))
      else {
        def resetSession(session: JsValue) = {
          session.as[JsObject] ++
            Json.obj("isComplete" -> false) ++
            Json.obj("components" -> Json.obj()) ++
            Json.obj("attempts" -> 0)
        }

        ss.saveSession(id, resetSession(ss.existingSession)).map {
          savedSession =>
            updatesLogger.trace(s"reset - session has been saved as: $savedSession")
            Ok(savedSession)
        }.getOrElse(BadRequest("Error saving resetted session"))
      }
    })
  }

  def reopenSession(id: String) = Action.async { implicit request =>
    hooks.save(id).map(basicHandler { ss =>
      if (ss.isSecure)
        BadRequest(Json.obj("error" -> JsString(Errors.cantReopenSecureSession)))
      else {
        def reopenSession(session: JsValue) = {
          session.as[JsObject] ++
            Json.obj("isComplete" -> false) ++
            Json.obj("attempts" -> 0)
        }

        ss.saveSession(id, reopenSession(ss.existingSession)).map {
          savedSession =>
            updatesLogger.trace(s"reopen - session has been saved as: $savedSession")
            Ok(savedSession)
        }.getOrElse(BadRequest("Error saving reopened session"))
      }
    })
  }

  def loadItemAndSession(sessionId: String) = Action.async {
    implicit request =>
      Future {
        val response = hooks.loadItemAndSession(sessionId)

        response match {
          case Left(err) => err
          case Right(fs) => {
            val json = fs.everything

            val itemJson = (json \ "item").as[JsObject]
            val processedItem = itemPreProcessor.preProcessItemForPlayer(itemJson)
            val sessionJson = (json \ "session").as[JsObject]

            val base = Json.obj(
              "item" -> processedItem,
              "session" -> sessionJson)
            loadLogger.trace(s"loadItemAndSession: $base")
            Ok(base)
          }
        }
      }
  }

  def saveSession(id: String) = Action.async { implicit request =>
    hooks.save(id).map(basicHandler { ss =>

      logger.trace(s"[saveSession] : $id")

      if (ss.isSecure && ss.isComplete)
        BadRequest(Json.obj("error" -> JsString(Errors.cantSaveWhenComplete)))
      else {
        request.body.asJson.map {
          requestJson =>

            val isAttempt: Boolean = (requestJson \ "isAttempt").asOpt[Boolean].getOrElse(false)
            val isComplete: Boolean = (requestJson \ "isComplete").asOpt[Boolean].getOrElse(false)

            val attemptUpdate = if (isAttempt) {
              val currentCount = (ss.existingSession \ "attempts").asOpt[Int].getOrElse(0)
              Json.obj("attempts" -> JsNumber(currentCount + 1))
            } else Json.obj()

            val completeUpdate = if (isComplete) Json.obj("isComplete" -> JsBoolean(true)) else Json.obj()

            val update = ss.existingSession.as[JsObject] ++
              Json.obj("components" -> requestJson \ "components") ++
              attemptUpdate ++ completeUpdate

            ss.saveSession(id, update).map {
              savedSession =>
                updatesLogger.trace(s"session has been saved as: $savedSession")
                Ok(savedSession)
            }.getOrElse(BadRequest("Error saving"))
        }.getOrElse(BadRequest("No session in the request body"))
      }
    })
  }

  /**
   * Load outcome for a session.
   *
   * @param id
   * request body : json - a set of evaluation options to be passed in to the outcome processors
   * @return
   */
  def loadOutcome(id: String) = Action.async {
    implicit request =>
      Future {
        val reponse = hooks.loadOutcome(id)

        reponse match {
          case Left(err) => InternalServerError(err._2)
          case Right(so) => {
            logger.trace(s"[loadOutcome]: $id : ${Json.stringify(so.itemSession)}")

            def hasAnswers = (so.itemSession \ "components").asOpt[JsObject].isDefined

            if (so.isSecure && !so.isComplete) {
              BadRequest(Json.obj("error" -> JsString("secure mode: can't load outcome - session isn't complete")))
            } else {
              request.body.asJson.map { settings =>
                val outcome = outcomeProcessor.createOutcome(so.item, so.itemSession, settings)
                val score = scoreProcessor.score(so.item, so.itemSession, outcome)
                val result = Json.obj("outcome" -> outcome) ++ Json.obj("score" -> score) ++ (hasAnswers match {
                  case false => Json.obj("warning" -> "this session contains no answers")
                  case true => Json.obj()
                })
                logScore("loadOutcome", score, so.itemSession, so.item)
                Ok(result)
              }.getOrElse {
                BadRequest(Json.obj("error" -> "No settings in request body"))
              }
            }
          }
        }
      }(sessionContext.heavyLoad)
  }
  /**
   * Load instructor data for a session.
   *
   * @param id
   * @return
   */
  def loadInstructorData(id: String) = Action {
    implicit request =>
      {
        val reponse = hooks.loadOutcome(id)

        reponse match {
          case Left(err) => InternalServerError(err._2)
          case Right(so) => {
            logger.trace(s"[loadAnswerKey]: $id : ${Json.stringify(so.item)} : ${Json.stringify(so.itemSession)}")

            if (so.isSecure && !so.isComplete) {
              BadRequest(Json.obj("error" -> JsString("secure mode: can't load instructor data - session isn't complete")))
            } else {
              Ok(Json.obj("item" -> so.item, "session" -> so.itemSession))
            }
          }
        }
      }
  }

  /**
   * Be aware that this is two different implementations melted into one api.
   * The http route for it is a PUT which allows the user to pass in the answers.
   * When secure=false these answers are used to calculate the result
   * When secure=true the answers are ignored and the answers from the session are used instead.
   *
   * @param id
   * @return
   */
  def getScore(id: String) = Action.async {
    implicit request =>
      Future {
        val response = hooks.getScore(id)

        response match {
          case Left(err) => InternalServerError(err._2)
          case Right(so) => {
            val answers: Either[String, JsValue] = {

              if (so.isSecure) {
                if (!so.isComplete) {
                  Left("Can't get score if session has not been completed")
                } else {
                  (so.itemSession \ "components").asOpt[JsValue].map { _ =>
                    Right(so.itemSession)
                  }.getOrElse(Left("Can't calculate a score if there are no saved answers"))
                }
              } else {
                request.body.asJson
                  .orElse((so.itemSession \ "components").asOpt[JsValue].map(_ => so.itemSession))
                  .map(Right(_))
                  .getOrElse(Left("Can't find answers in request body or in the db"))
              }
            }

            answers match {
              case Left(err) => BadRequest(Json.obj("error" -> err))
              case Right(a) =>
                val outcome = outcomeProcessor.createOutcome(so.item, a, Json.obj())
                val score = scoreProcessor.score(so.item, a, outcome)
                logScore("getScore", score, a, so.item)
                Ok(score)
            }
          }
        }
      }(sessionContext.heavyLoad)
  }

  def completeSession(id: String) = Action.async { implicit request =>
    hooks.save(id).map(basicHandler({ (ss: SaveSession) =>
      val sessionJson = ss.existingSession.as[JsObject] ++ Json.obj("isComplete" -> JsBoolean(true))
      ss.saveSession(id, sessionJson).map {
        savedSession =>
          {
            updatesLogger.trace(s"[completeSession]: $id : $savedSession")
            Ok(savedSession)
          }
      }.getOrElse(BadRequest("Error completing"))
    }))
  }

}
