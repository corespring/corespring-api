package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.libs.json.Json
import api.ApiError
import org.bson.types.ObjectId
import models._
import org.joda.time.DateTime
import com.novus.salat.dao.{SalatInsertError, SalatSaveError}
import play.api.Logger
import com.mongodb.casbah.MongoCursor
import play.api.mvc.{AnyContent, Result}
import com.mongodb.casbah.Imports._
import controllers.{Log, Utils}
import scala.Left
import scala.Some
import scala.Right


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
  def getItemSession(itemId:ObjectId, sessionId: ObjectId) = ApiAction { request =>
    ItemSession.findOneById(sessionId) match {
      case Some(o) => {
        if(Content.isAuthorized(request.ctx.organization, o.itemId, Permission.All)){
          Ok(Json.toJson(o))
        }else Unauthorized(Json.toJson(ApiError.UnauthorizedItemSession))
        Ok(Json.toJson(o))
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
    if (Content.isAuthorized(request.ctx.organization,itemId,Permission.All)){
      val newSession = request.body.asJson match {
        case Some(jssession) => Json.fromJson[ItemSession](jssession)
        case None => ItemSession(itemId)
      }
      ItemSession.newItemSession(itemId,newSession) match {
        case Right(session) => Ok(Json.toJson(session))
        case Left(error) => InternalServerError(Json.toJson(ApiError.CreateItemSession(error.clientOutput)))
      }
    }else Unauthorized(Json.toJson(ApiError.UnauthorizedItemSession))
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

}
