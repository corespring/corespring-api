package api.v1

import controllers.auth.BaseApi
import play.api.libs.json.Json
import api.ApiError
import org.bson.types.ObjectId
import models.{ItemSession, ItemResponse, ContentCollection}
import org.joda.time.DateTime
import com.novus.salat.dao.SalatSaveError
import play.api.Logger

/**
 *  API for managing item sessions
 */
object ItemSessionApi extends BaseApi {

  /**
   * Serves POST request
   * Creates an itemSession.
   * Does not require a json body, by default will create an 'empty' session for the item id
   *
   * @return json for the created
   */
  def createItemSession(itemId: ObjectId) = ApiAction { request =>
    request.body.asJson match {
      case Some(json) => {
        (json \ "id").asOpt[String] match {
          case Some(id) => BadRequest(Json.toJson(ApiError.IdNotNeeded))
          case _ => {
            // no id received, but we did get some json...
            val start = (json \ "start").asOpt[String]
            val finish = (json \ "finish").asOpt[String]
            val responses = (json \ "responses").asOpt[List[ItemResponse]]


            // TODO - need to load the itemId and make sure an item exists with that id. illegal to create a session without an item


            if ( start.isEmpty ) {
              BadRequest( Json.toJson(ApiError.ItemSessionRequiredFields))
            } else {
              val itemSession = ItemSession(Some(new ObjectId()), itemId)
              val startTime = new DateTime(start.getOrElse("").toLong)
              itemSession.start = startTime
              if (! finish.isEmpty) itemSession.finish = new DateTime(finish.getOrElse("").toLong)
              itemSession.responses = responses.getOrElse(List[ItemResponse]())

              insert(itemSession) match {
                case Right(session) => Ok(Json.toJson(session))
                case Left(error) => InternalServerError(Json.toJson(error))
              }

            }
          }
        }
      }
      case _ => {
        // no json, create an empty session which will be initialized to current start time
        val itemSession = ItemSession(Some(new ObjectId()), itemId)
        insert(itemSession) match {
          case Right(session) => Ok(Json.toJson(session))
          case Left(error) => InternalServerError(Json.toJson(error))
        }
      }
    }
  }

  /**
   * insert an itemSession
   * @param itemSession
   * @return
   */
  def insert( itemSession: ItemSession): Either[ApiError, ItemSession] = {
    try {
      ItemSession.save(itemSession)
      Right(itemSession)
    } catch {
      case e: SalatSaveError => {
        Left(ApiError.OperationError)
      }
    }
  }

}
