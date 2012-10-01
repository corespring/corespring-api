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
import controllers.testplayer.qti.QtiItem
import com.novus.salat._
import models.mongoContext._
import play.api.cache.Cache
import controllers.testplayer.{ItemSessionXmlStore, ItemPlayer}
import xml.Elem
import play.api.Play.current
import play.api.Logger


/**
 *  API for managing item sessions
 */
object ItemSessionApi extends BaseApi {


  def list(itemId:ObjectId) = ApiAction { request =>
    if(Content.isAuthorized(request.ctx.organization,itemId,Permission.All)){
      val cursor = ItemSession.find(MongoDBObject(ItemSession.itemId -> itemId))
      Ok(Json.toJson(Utils.toSeq(cursor)))
    }else Unauthorized(Json.toJson(ApiError.UnauthorizedItemSession))
  }
  /**
   *
   * Serves GET request
   * @param sessionId
   * @return
   */
  def getItemSession(itemId: ObjectId, sessionId: ObjectId) = ApiAction { request =>
    ItemSession.findOneById(sessionId) match {
      case Some(itemSession) => {
        if (Content.isAuthorized(request.ctx.organization, itemSession.itemId, Permission.All)) {
          if(itemSession.finish.isDefined){

            val cachedXml : Option[Elem] =  ItemSessionXmlStore.getCachedXml(itemId.toString, sessionId.toString)

            cachedXml match {
              case Some(xml) => {
                itemSession.sessionData = ItemSession.getSessionData(xml,itemSession.responses)
              }
              case _ => NotFound(Json.toJson(ApiError.ItemSessionNotFound))
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
  def createItemSession(itemId: ObjectId) = ApiAction { request =>
    if (Content.isAuthorized(request.ctx.organization,itemId,Permission.All)) {
      val newSession = request.body.asJson match {
        case Some(jssession) => Json.fromJson[ItemSession](jssession)
        case None => ItemSession(itemId)
      }

      ItemSession.newItemSession(itemId,newSession) match {
        case Right(session) => {

          /**
           * Temporarily - process the raw xml and add csFeedbackIds
           * Then cache it.
           */
          getQtiXml(itemId) match {
            case Some(xml) => {
              val xmlWithFeedbackIds = ItemSessionXmlStore.addCsFeedbackIds(xml)
              ItemSessionXmlStore.cacheXml(xmlWithFeedbackIds, itemId.toString, session.id.toString)
              Ok(Json.toJson(session))
            }
            case _ => {
              Logger.warn("Returning session - no xml cached!")
              Ok(Json.toJson(session))
            }
          }
        }
        case Left(error) => InternalServerError(Json.toJson(ApiError.CreateItemSession(error.clientOutput)))
      }
    } else {
      Unauthorized(Json.toJson(ApiError.UnauthorizedItemSession))
    }
  }


  private def getQtiXml(itemId:ObjectId) : Option[Elem] = {
    Item.findOneById(itemId) match {
      case Some(item) => {
        val dataResource = item.data.get
        dataResource.files.find( _.name == Resource.QtiXml ) match {
          case Some(qtiXml) => Some( scala.xml.XML.loadString(qtiXml.asInstanceOf[VirtualFile].content))
          case _ => None
        }
      }
      case _ => None
    }
  }

  private def getSessionFeedback(itemId: ObjectId, itemSession: ItemSession): Map[String, String] = {
    Item.getQti(itemId) match {
      case Right(xmlData) => optMap[String, String](
        new QtiItem(scala.xml.XML.loadString(xmlData)).feedback(itemSession.responses).map(feedback => (feedback.csFeedbackId, feedback.body))
      ).getOrElse(Map[String, String]())
      case Left(error) => Map()
    }
  }
  /**
   * Serves the PUT request for an item session
   * @param itemId
   */
  def updateItemSession(itemId: ObjectId, sessionId:ObjectId) = ApiAction { request =>
    ItemSession.findOneById(sessionId) match {
      case Some(session) => Content.isAuthorized(request.ctx.organization,session.itemId,Permission.All) match {
        case true => request.body.asJson match {
          case Some(jssession) => {
            val newSession = Json.fromJson[ItemSession](jssession)
            session.finish = newSession.finish
            session.responses = newSession.responses

            /**
             * This is a temporary means of allowing SessionData and itemplayer
             * To use the same xml with csFeedbackId attributes.
             */
            val cachedXml : Option[Elem] = ItemSessionXmlStore.getCachedXml(itemId.toString, session.id.toString)

            cachedXml match {
              case Some(xmlWithCsFeedbackIds) => {
                ItemSession.updateItemSession(session, xmlWithCsFeedbackIds) match {
                  case Right(newSession) => Ok(Json.toJson(newSession))
                  case Left(error) => InternalServerError(Json.toJson(ApiError.UpdateItemSession(error.clientOutput)))
                }
              }
              case _ => InternalServerError(Json.toJson(ApiError.UpdateItemSession(Some("can't find cached xml"))))
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
  private def optMap[A,B](in: Iterable[(A,B)]): Option[Map[A,B]] =
    in.iterator.foldLeft(Option(Map[A,B]())) {
      case (Some(m),e @ (k,v)) if m.getOrElse(k, v) == v => Some(m + e)
      case _ => None
    }

}
