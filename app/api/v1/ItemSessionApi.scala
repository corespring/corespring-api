package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.libs.json.Json
import api.ApiError
import org.bson.types.ObjectId
import models._
import com.mongodb.casbah.Imports._
import controllers.{Log, Utils}
import scala.Left
import scala.Some
import scala.Right
import xml.Elem
import play.api.Play.current
import play.api.Logger
import qti.processors.FeedbackProcessor


/**
 * API for managing item sessions
 */
object ItemSessionApi extends BaseApi {


  def list(itemId: ObjectId) = ApiAction {
    request =>
      if (Content.isAuthorized(request.ctx.organization, itemId, Permission.All)) {
        val cursor = ItemSession.find(MongoDBObject(ItemSession.itemId -> itemId))
        Ok(Json.toJson(Utils.toSeq(cursor)))
      } else Unauthorized(Json.toJson(ApiError.UnauthorizedItemSession))
  }

  /**
   *
   * Serves GET request
   * @param sessionId
   * @return
   */
  def getItemSession(itemId: ObjectId, sessionId: ObjectId) = ApiAction {
    request =>
      ItemSession.findOneById(sessionId) match {
        case Some(itemSession) => {
          if (Content.isAuthorized(request.ctx.organization, itemSession.itemId, Permission.All)) {
            if (itemSession.finish.isDefined) {

              //val cachedXml: Option[Elem] = ItemSessionXmlStore.getCachedXml(itemId.toString, sessionId.toString)
              ItemSession.getXmlWithFeedback(itemId,itemSession.feedbackIdLookup) match {
                case Right(xml) => {
                  itemSession.sessionData = ItemSession.getSessionData(xml, itemSession.responses)
                }
                case Left(e) => NotFound(Json.toJson(ApiError.ItemSessionNotFound(e.clientOutput)))
              }
            }
            Ok(Json.toJson(itemSession))
          }
          else {
            Unauthorized(Json.toJson(ApiError.UnauthorizedItemSession))
          }
        }
        case None => NotFound
      }

  }

  /**
   * Serves POST request
   * Creates an itemSession.
   * Does not require a json body, by default will create an 'empty' session for the item id
   *
   * @return json for the created item session
   */
  def createItemSession(itemId: ObjectId) = ApiAction {
    request =>
      if (Content.isAuthorized(request.ctx.organization, itemId, Permission.All)) {
        val newSession = request.body.asJson match {
          case Some(json) => {
            val jsonSession = Json.fromJson[ItemSession](json)
            jsonSession.id = itemId
            jsonSession
          }
          case None => ItemSession(itemId)
        }
        getQtiXml(itemId) match {
          case Some(xml) => {
            val (xmlWithFeedbackIds,mapping) = FeedbackProcessor.addFeedbackIds(xml)
            newSession.feedbackIdLookup = mapping
          }
          case _ =>
        }
        ItemSession.newItemSession(itemId, newSession) match {
          case Right(session) => Ok(Json.toJson(session))
          case Left(error) => InternalServerError(Json.toJson(ApiError.CreateItemSession(error.clientOutput)))
        }
      } else {
        Unauthorized(Json.toJson(ApiError.UnauthorizedItemSession))
      }
  }


  private def getQtiXml(itemId: ObjectId): Option[Elem] = {
    Item.findOneById(itemId) match {
      case Some(item) => {
        val dataResource = item.data.get
        dataResource.files.find(_.name == Resource.QtiXml) match {
          case Some(qtiXml) => Some(scala.xml.XML.loadString(qtiXml.asInstanceOf[VirtualFile].content))
          case _ => None
        }
      }
      case _ => None
    }
  }

  def begin(itemId:ObjectId, sessionId:ObjectId) = ApiAction {
    request =>
      findSessionAndCheckAuthorization(sessionId, itemId, request.ctx.organization) match {
        case Right(s) => {
          ItemSession.startItemSession(s) match {
            case Left(error) => BadRequest(error.message)
            case Right(started) => Ok(Json.toJson(started))
          }
        }
        case Left(error) => BadRequest(Json.toJson(error))
      }
  }

  /**
   * Find the session and check the user is authorized to manipulate it.
   * @param sessionId
   * @param itemId
   * @param orgId
   * @return
   */
  private def findSessionAndCheckAuthorization(sessionId:ObjectId, itemId: ObjectId, orgId : ObjectId ) : Either[ApiError,ItemSession] = ItemSession.findOneById(sessionId) match {
    case Some(s) => Content.isAuthorized(orgId, itemId, Permission.All) match {
      case true => Right(s)
      case false => Left( ApiError.UnauthorizedItemSession )
    }
    case None => Left( ApiError.ItemSessionNotFound )
  }

  /**
   * Serves the PUT request for an item session
   * @param itemId
   */
  def updateItemSession(itemId: ObjectId, sessionId: ObjectId) = ApiAction {
    request =>
      ItemSession.findOneById(sessionId) match {
        case Some(dbSession) => Content.isAuthorized(request.ctx.organization, dbSession.itemId, Permission.All) match {
          case true => request.body.asJson match {
            case Some(jsonSession) => {

              if (dbSession.finish.isDefined) {
                BadRequest(Json.toJson(ApiError.ItemSessionFinished))
              } else {

                val clientSession = Json.fromJson[ItemSession](jsonSession)
                dbSession.finish = clientSession.finish
                dbSession.responses = clientSession.responses
                dbSession.settings = clientSession.settings

                ItemSession.getXmlWithFeedback(itemId,dbSession.feedbackIdLookup) match {
                  case Right(xmlWithCsFeedbackIds) => {
                    ItemSession.updateItemSession(dbSession, xmlWithCsFeedbackIds) match {
                      case Right(newSession) => {
                        val json = Json.toJson(newSession)
                        Ok(json)
                      }
                      case Left(error) => InternalServerError(Json.toJson(ApiError.UpdateItemSession(error.clientOutput)))
                    }
                  }
                  case Left(e) => InternalServerError(Json.toJson(ApiError.UpdateItemSession(e.clientOutput)))
                }
              }
            }
            case None => BadRequest(Json.toJson(ApiError.JsonExpected))
          }
          case false => Unauthorized(Json.toJson(ApiError.UnauthorizedItemSession))
        }
        case None => BadRequest(Json.toJson(ApiError.ItemSessionNotFound))
      }
  }

  // Translates a collection of tuples to an Option[Map]
  private def optMap[A, B](in: Iterable[(A, B)]): Option[Map[A, B]] =
    in.iterator.foldLeft(Option(Map[A, B]())) {
      case (Some(m), e@(k, v)) if m.getOrElse(k, v) == v => Some(m + e)
      case _ => None
    }

}
