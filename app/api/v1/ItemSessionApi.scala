package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.libs.json.Json
import api.ApiError
import org.bson.types.ObjectId
import models._
import com.mongodb.casbah.Imports._
import controllers.Utils
import scala.Left
import scala.Some
import scala.Right
import controllers.testplayer.qti.QtiItem


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
          Item.collection.findOneByID(itemId, MongoDBObject(Item.data -> 1)) match {
            case Some(o) => {
              val xmlData = o.get(Item.data).toString
              val qtiItem = new QtiItem(scala.xml.XML.loadString(xmlData))
              Ok(Json.toJson(itemSession))
            }
            case None => NotFound
          }
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
          val feedback: Map[String, String] = getSessionFeedback(itemId, newSession)
          if (!feedback.isEmpty) {
            Ok(Json.toJson(session.sessionData(Map("feedbackContent" -> feedback))))
          }
          else {
            Ok(Json.toJson(session))
          }
        }
        case Left(error) => InternalServerError(Json.toJson(ApiError.CreateItemSession(error.clientOutput)))
      }
    }
    else {
      Unauthorized(Json.toJson(ApiError.UnauthorizedItemSession))
    }
  }

  private def getSessionFeedback(itemId: ObjectId, itemSession: ItemSession): Map[String, String] = {
    Item.collection.findOneByID(itemId, MongoDBObject(Item.data -> 1)) match {
      case None => Map[String, String]()
      case Some(o) => {
        val xmlData = o.get(Item.data).toString
        val qtiItem = new QtiItem(scala.xml.XML.loadString(xmlData))

        optMap[String, String](
          qtiItem.feedback(itemSession.responses).map(feedback => (feedback.csFeedbackId, feedback.body))
        ).getOrElse(Map[String, String]())
      }
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
            ItemSession.updateItemSession(session) match {
              case Right(_) => Ok(Json.toJson(session))
              case Left(error) => InternalServerError(Json.toJson(ApiError.UpdateItemSession(error.clientOutput)))
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
