package api.v1

import api.ApiError
import com.mongodb.casbah.Imports._
import controllers.auth.ApiRequest
import controllers.auth.BaseApi
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Content
import org.corespring.platform.core.models.itemSession._
import org.corespring.platform.core.services.item.{ ItemServiceImpl, ItemService }
import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.cookies.PlayerCookieReader
import org.corespring.qti.models.responses.ArrayResponse
import org.corespring.qti.models.responses.ResponseAggregate
import org.corespring.qti.models.responses.StringResponse
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc.AnyContent
import scala.Left
import scala.Right
import scala.Some

/**
 * API for managing item sessions
 */

class ItemSessionApi(itemSession: ItemSessionCompanion, itemService: ItemService, quizService: QuizService) extends BaseApi with PlayerCookieReader {

  def aggregate(quizId: ObjectId, itemId: VersionedId[ObjectId]) = ApiAction {
    request =>

      quizService.findOneById(quizId) match {
        case Some(q) =>
          val sessions = q.participants.map(_.answers.filter(_.itemId.toString == itemId.toString).map(_.sessionId.toString)).flatten
          Ok(JsObject(aggregateSessions(sessions).toList.map(p => (p._1, toJson(p._2)))))

        case _ => NotFound
      }
  }

  private def aggregateSessions(sessionIds: Seq[String]): Map[String, ResponseAggregate] = {
    val agg: scala.collection.mutable.Map[String, ResponseAggregate] = scala.collection.mutable.Map()
    sessionIds.foreach {
      p =>
        val oid = new ObjectId(p)
        itemSession.get(oid)(false) match {
          case Some(session) => {
            session.responses.foreach {
              resp =>
                if (agg.contains(resp.id)) {
                  agg(resp.id) = agg(resp.id).aggregate(resp)
                } else {
                  val correctResponses = session.sessionData match {
                    case Some(sd) => sd.correctResponses
                    case _ => Seq()
                  }
                  val cr = correctResponses.find(_.id == resp.id) match {
                    case Some(r: ArrayResponse) => r.responseValue
                    case Some(r: StringResponse) => Seq(r.responseValue)
                    case _ => Seq()
                  }
                  agg(resp.id) = ResponseAggregate.build(resp.id, cr, resp)
                }
            }
          }
          case _ => {
            Seq()
          }
        }
    }
    agg.toMap
  }

  def list(itemId: VersionedId[ObjectId]) = ApiAction {
    request =>
      if (Content.isAuthorized(request.ctx.organization, itemId, Permission.Read)) {
        val cursor = itemSession.find(MongoDBObject(ItemSession.Keys.itemId -> itemId))
        Ok(toJson(cursor.toSeq))
      } else Unauthorized(toJson(ApiError.UnauthorizedItemSession))
  }

  def update(itemId: VersionedId[ObjectId], sessionId: ObjectId, role:String, action: Option[String]) = action match {
    case Some("begin") => begin(itemId, sessionId)
    case Some("updateSettings") => updateSettings(itemId, sessionId)
    case _ => processResponse(itemId, sessionId)(role)
  }

  def reopen(itemId: VersionedId[ObjectId], sessionId: ObjectId) = ApiAction{ request =>
    findSessionAndCheckAuthorization(sessionId, itemId, request.ctx.organization) match {
      case Left(error) => BadRequest(toJson(error))
      case Right(session) => {
        itemSession.reopen(session).map{ update => Ok(toJson(update)) }.getOrElse{
          BadRequest(toJson(ApiError.ReopenItemSessionFailed))
        }
      }
    }
  }

  /**
   * @param sessionId
   * @return
   */
  def get(itemId: VersionedId[ObjectId], sessionId: ObjectId, role:String) = ApiAction {
    request =>
      implicit val isInstructor = role == "instructor"
      itemSession.get(sessionId) match {
        case Some(session) => {
          if (Content.isAuthorized(request.ctx.organization, session.itemId, Permission.Read)) {
            if (isInstructor) session.settings = session.settings.copy(highlightCorrectResponse = true, highlightUserResponse = true, showFeedback = true)
            Ok(toJson(session))
          } else {
            Unauthorized(toJson(ApiError.UnauthorizedItemSession))
          }
        }
        case _ => NotFound
      }
  }

  /**
   * Creates an itemSession.
   *
   * @return json for the created item session
   */
  def create(itemId: VersionedId[ObjectId]) = ApiAction {
    request =>

      def getSettings(json: JsValue): Option[ItemSessionSettings] = {
        (json \ "settings") match {
          case obj: JsObject => obj.asOpt[ItemSessionSettings]
          case _ => Some(ItemSessionSettings())
        }
      }

      if (Content.isAuthorized(request.ctx.organization, itemId, Permission.Read)) {
        //if the version is not included, we need the current version to include in the itemId in session
        val currentItemId: VersionedId[ObjectId] = if (itemId.version.isDefined) itemId else
          itemService.findOneById(itemId).get.id //don't bother checking if item exists since we already did that in Content.isAuthorized

        val s: Option[ItemSession] = for {
          json <- request.body.asJson.orElse(Some(JsObject(Seq())))
          settings <- getSettings(json)
        } yield ItemSession(itemId = currentItemId, settings = settings)

        s.map { session =>
          itemSession.newSession(session) match {
            case Right(saved) => Ok(toJson(saved))
            case Left(error) => InternalServerError(toJson(ApiError.CreateItemSession(error.clientOutput)))
          }
        }.getOrElse(BadRequest("Error creating item session"))
      } else {
        Unauthorized(toJson(ApiError.UnauthorizedItemSession))
      }
  }

  def begin(itemId: VersionedId[ObjectId], sessionId: ObjectId) = ApiAction {
    request =>
      findSessionAndCheckAuthorization(sessionId, itemId, request.ctx.organization) match {
        case Right(s) => {
          itemSession.begin(s) match {
            case Left(error) => BadRequest(error.message)
            case Right(started) => Ok(toJson(started))
          }
        }
        case Left(error) => BadRequest(toJson(error))
      }
  }

  /**
   * Find the session and check the user is authorized to manipulate it.
   * @param sessionId
   * @param itemId
   * @param orgId
   * @return
   */
  private def findSessionAndCheckAuthorization(sessionId: ObjectId, itemId: VersionedId[ObjectId], orgId: ObjectId): Either[ApiError, ItemSession] = itemSession.findOneById(sessionId) match {
    case Some(s) => Content.isAuthorized(orgId, itemId, Permission.Read) match {
      case true => Right(s)
      case false => Left(ApiError.UnauthorizedItemSession)
    }
    case None => Left(ApiError.ItemSessionNotFound)
  }

  /**
   * Update the item session - note this only updates the settings
   * It is different from when a user submits responses for this session
   * @param itemId
   * @param sessionId
   * @return
   */
  def updateSettings(itemId: VersionedId[ObjectId], sessionId: ObjectId) = ApiAction {
    request =>
      findSessionAndCheckAuthorization(sessionId, itemId, request.ctx.organization) match {
        case Left(error) => BadRequest(toJson(error))
        case Right(dbSession) => {
          requestAsData(request, ApiError.ItemSessionRequiredFields) match {
            case Left(error) => BadRequest(toJson(error))
            case Right(update) => {
              itemSession.update(update) match {
                case Left(error) => BadRequest(toJson(ApiError.CantSave))
                case Right(updatedSession) => Ok(toJson(updatedSession))
              }
            }
          }
        }
      }
  }

  /**
   * Parse the request body as an ItemSession
   * @param request
   * @return
   */
  private def requestAsData(request: ApiRequest[AnyContent], error: ApiError): Either[ApiError, ItemSession] = {
    request.body.asJson match {
      case Some(json) => {
        json.asOpt[ItemSession] match {
          case Some(is) => Right(is)
          case _ => Left(error)
        }
      }
      case _ => Left(error)
    }
  }

  /**
   * Process the user response - this counts as an attempt at the question
   * Return sessionData and ResponseOutcomes
   * @param itemId
   */
  def processResponse(itemId: VersionedId[ObjectId], sessionId: ObjectId)(implicit role:String) = ApiAction {
    request =>
      implicit val isInstructor = role == "instructor"
      logger.debug("[processResponse]: " + sessionId)

      itemSession.findOneById(sessionId) match {
        case Some(dbSession) => Content.isAuthorized(request.ctx.organization, dbSession.itemId, Permission.Read) match {
          case true => request.body.asJson match {
            case Some(jsonSession) => {

              if (dbSession.finish.isDefined) {
                BadRequest(toJson(ApiError.ItemSessionFinished))
              } else {

                fromJson[ItemSession](jsonSession) match {
                  case JsSuccess(clientSession, _) =>
                    {
                      val isAttempt = (jsonSession \ "isAttempt").asOpt[Boolean].getOrElse(true)
                      dbSession.finish = clientSession.finish
                      dbSession.responses = clientSession.responses

                      itemSession.getXmlWithFeedback(dbSession) match {
                        case Right(xmlWithCsFeedbackIds) => {
                          itemSession.process(dbSession, xmlWithCsFeedbackIds,isAttempt) match {
                            case Right(newSession) => {
                              if (isInstructor) newSession.settings = newSession.settings.copy(highlightCorrectResponse = true, highlightUserResponse = true, showFeedback = true)
                              val json = toJson(newSession)
                              logger.debug("[processResponse] successful")
                              Ok(json)
                            }
                            case Left(error) => InternalServerError(toJson(ApiError.UpdateItemSession(error.clientOutput)))
                          }
                        }
                        case Left(e) => InternalServerError(toJson(ApiError.UpdateItemSession(e.clientOutput)))
                      }
                    }
                  case JsError(e) => BadRequest("") //?
                }
              }
            }
            case None => BadRequest(toJson(ApiError.JsonExpected))
          }
          case false => Unauthorized(toJson(ApiError.UnauthorizedItemSession))
        }
        case None => BadRequest(toJson(ApiError.ItemSessionNotFound))
      }
  }
}

object ItemSessionApi extends ItemSessionApi(DefaultItemSession, ItemServiceImpl, QuizService)
