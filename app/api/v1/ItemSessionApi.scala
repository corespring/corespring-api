package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.libs.json.Json._
import api.ApiError
import models._
import com.mongodb.casbah.Imports._
import controllers.{Log, Utils}
import scala.Left
import scala.Some
import scala.Right
import play.api.mvc.AnyContent
import play.api.libs.json.JsObject


/**
 * API for managing item sessions
 */
object ItemSessionApi extends BaseApi {


  def aggregate = ApiAction {
    request =>
      request.body.asJson match {
        case Some(x) =>
          val sessionIds = (x \ "sessions").as[List[String]]
          val list = aggregateSessions(sessionIds).toList.map(p => (p._1, toJson(p._2)))
          Ok(JsObject(list))

        case None =>
          BadRequest("Need session ids in POST payload")
      }

  }

  private def aggregateSessions(sessionIds: List[String]): Map[String, ItemResponseAggregate] = {
    val agg: scala.collection.mutable.Map[String, ItemResponseAggregate] = scala.collection.mutable.Map()
    sessionIds.foreach {
      p =>
        val oid = new ObjectId(p)
        ItemSession.get(oid) match {
          case Some(session) =>
            session.responses.foreach {
              resp =>
                if (agg.contains(resp.id)) {
                  agg(resp.id) = agg(resp.id).aggregate(resp)
                } else {
                  val correctResponses = session.sessionData match {
                    case Some(sd) => sd.correctResponses
                    case None => Seq()
                  }
                  val cr = correctResponses.find(_.id == resp.id) match {
                    case Some(r: ArrayItemResponse) => r.responseValue
                    case Some(r: StringItemResponse) => Seq(r.responseValue)
                    case None => Seq()
                  }
                  agg(resp.id) = ItemResponseAggregate(resp.id, cr)
                }
            }
        }
    }
    agg.toMap
  }


  def list(itemId: ObjectId) = ApiAction {
    request =>
      if (Content.isAuthorized(request.ctx.organization, itemId, Permission.Read)) {
        val cursor = ItemSession.find(MongoDBObject(ItemSession.itemId -> itemId))
        Ok(toJson(Utils.toSeq(cursor)))
      } else Unauthorized(toJson(ApiError.UnauthorizedItemSession))
  }


  def update(itemId: ObjectId, sessionId: ObjectId, action: Option[String]) = action match {
    case Some("begin") => begin(itemId, sessionId)
    case Some("updateSettings") => updateSettings(itemId, sessionId)
    case _ => processResponse(itemId, sessionId)
  }


  /**
   * @param sessionId
   * @return
   */
  def get(itemId: ObjectId, sessionId: ObjectId) = ApiAction {
    request =>
      ItemSession.get(sessionId) match {
        case Some(session) => {
          if (Content.isAuthorized(request.ctx.organization, session.itemId, Permission.Read)) {
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
  def create(itemId: ObjectId) = ApiAction {
    request =>
      if (Content.isAuthorized(request.ctx.organization, itemId, Permission.Read)) {
        val newSession = request.body.asJson match {
          case Some(json) => {
            val jsonSession = fromJson[ItemSession](json)
            //We only pull in the settings from the request
            ItemSession(itemId = itemId, settings = jsonSession.settings)
          }
          case None => ItemSession(itemId)
        }
        ItemSession.newSession(itemId, newSession) match {
          case Right(session) => Ok(toJson(session))
          case Left(error) => InternalServerError(toJson(ApiError.CreateItemSession(error.clientOutput)))
        }
      } else {
        Unauthorized(toJson(ApiError.UnauthorizedItemSession))
      }
  }


  def begin(itemId: ObjectId, sessionId: ObjectId) = ApiAction {
    request =>
      findSessionAndCheckAuthorization(sessionId, itemId, request.ctx.organization) match {
        case Right(s) => {
          ItemSession.begin(s) match {
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
  private def findSessionAndCheckAuthorization(sessionId: ObjectId, itemId: ObjectId, orgId: ObjectId): Either[ApiError, ItemSession] = ItemSession.findOneById(sessionId) match {
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
  def updateSettings(itemId: ObjectId, sessionId: ObjectId) = ApiAction {
    request =>
      findSessionAndCheckAuthorization(sessionId, itemId, request.ctx.organization)
      match {
        case Left(error) => BadRequest(toJson(error))
        case Right(dbSession) => {
          requestAsData(request, ApiError.ItemSessionRequiredFields) match {
            case Left(error) => BadRequest(toJson(error))
            case Right(update) => {
              ItemSession.update(update) match {
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
   * Return sessionData and ItemResponseOutcomes
   * @param itemId
   */
  def processResponse(itemId: ObjectId, sessionId: ObjectId) = ApiAction {
    request =>

      Log.d("processResponse: " + sessionId)

      ItemSession.findOneById(sessionId) match {
        case Some(dbSession) => Content.isAuthorized(request.ctx.organization, dbSession.itemId, Permission.Read) match {
          case true => request.body.asJson match {
            case Some(jsonSession) => {

              if (dbSession.finish.isDefined) {
                BadRequest(toJson(ApiError.ItemSessionFinished))
              } else {

                val clientSession = fromJson[ItemSession](jsonSession)
                dbSession.finish = clientSession.finish
                dbSession.responses = clientSession.responses

                ItemSession.getXmlWithFeedback(dbSession) match {
                  case Right(xmlWithCsFeedbackIds) => {
                    ItemSession.process(dbSession, xmlWithCsFeedbackIds) match {
                      case Right(newSession) => {
                        val json = toJson(newSession)
                        Ok(json)
                      }
                      case Left(error) => InternalServerError(toJson(ApiError.UpdateItemSession(error.clientOutput)))
                    }
                  }
                  case Left(e) => InternalServerError(toJson(ApiError.UpdateItemSession(e.clientOutput)))
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

  // Translates a collection of tuples to an Option[Map]
  private def optMap[A, B](in: Iterable[(A, B)]): Option[Map[A, B]] =
    in.iterator.foldLeft(Option(Map[A, B]())) {
      case (Some(m), e@(k, v)) if m.getOrElse(k, v) == v => Some(m + e)
      case _ => None
    }

}
