package api.v1

import controllers.auth.BaseApi
import play.api.libs.json.{JsValue, Json}
import models.{ContentCollection, Organization}
import org.bson.types.ObjectId
import api.ApiError
import play.api.mvc.Result
import com.novus.salat.dao.SalatSaveError

/**
 * The Collections API
 */

object CollectionApi extends BaseApi {
  /**
   * Returns a list of collections visible to the organization in the request context
   *
   * @return
   */
  def list() = ApiAction { request =>
    Ok(Json.toJson(ContentCollection.findAllFor(request.ctx.organization).toList))
  }

  /**
   * Returns a Collection by its id
   *
   * @param id The collection id
   * @return
   */
  def getCollection(id: ObjectId) = ApiAction { request =>
    ContentCollection.findOneById(id) match {
      case Some(org) =>  {
        // todo: check if this collection is visible to the caller?
        Ok(Json.toJson(org))
      }
      case _ => NotFound
    }
  }

  /**
   * Creates a Collection
   *
   * @return
   */
  def createCollection = ApiAction { request =>
    request.body.asJson match {
      case Some(json) => {
        (json \ "id").asOpt[String] match {
          case Some(id) => BadRequest(Json.toJson(ApiError.IdNotNeeded))
          case _ => {
            val newId = new ObjectId
            val name = (json \ "name").asOpt[String]
            if ( name.isEmpty ) {
              BadRequest( Json.toJson(ApiError.CollectionNameMissing))
            } else {
              val organizations = parseOrganizations(json, Seq.empty)
              val collection = ContentCollection(newId, name.get, organizations)
              doSave(collection)
            }
          }
        }
      }
      case _ => jsonExpected
    }
  }


  private def parseOrganizations(json: JsValue, elseValue: Seq[ObjectId]):Seq[ObjectId] = {
    (json \ "organizations").asOpt[Seq[String]].map( seq => seq.map( new ObjectId(_)) ).getOrElse(elseValue)
  }

  private def unknownCollection = NotFound(Json.toJson(ApiError.UnknownCollection))

  /**
   * Updates a collection
   *
   * @return
   */
  def updateCollection(id: ObjectId) = ApiAction { request =>
    ContentCollection.findOneById(id).map( original =>
    {
      request.body.asJson match {
        case Some(json) => {
          val name = (json \ "name").asOpt[String].getOrElse(original.name)
          val toUpdate = ContentCollection( original.id, name, parseOrganizations(json, original.organizations))
          doSave(toUpdate)
        }
        case _ => jsonExpected
      }
    }).getOrElse(unknownCollection)
  }

  /**
   * Deletes a collection
   */
  def deleteCollection(id: ObjectId) = ApiAction { request =>
    ContentCollection.findOneById(id) match {
      case Some(toDelete) => {
        ContentCollection.removeById(id)
        Ok(Json.toJson(toDelete))
      }
      case _ => unknownCollection
    }
  }

  /**
   * Internal method to save a collection
   *
   * @param collection
   * @return
   */
  private def doSave(collection: ContentCollection): Result = {
    try {
      ContentCollection.save(collection)
      val newColl = ContentCollection.findOneById(collection.id)
      Ok(Json.toJson(newColl))
    } catch {
      case ex: SalatSaveError => InternalServerError(Json.toJson(ApiError.CantSave))
    }
  }
}
