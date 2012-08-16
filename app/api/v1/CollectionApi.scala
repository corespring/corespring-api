package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.libs.json.{JsObject, JsValue, Json}
import models.{ContentCollection, Organization}
import org.bson.types.ObjectId
import api.ApiError
import com.mongodb.casbah.commons.MongoDBObject

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
    val collids = ContentCollection.getCollectionIds(request.ctx.organization,Permission.All,false)

    val cursor = ContentCollection.find(MongoDBObject("_id" -> MongoDBObject("$in" -> collids)))

    val colls = cursor.toSeq
    cursor.close()
    Ok(Json.toJson(colls))
  }

  def listWithOrg(orgId: ObjectId) = ApiAction { request =>
    if (Organization.isChild(request.ctx.organization, orgId)){
      val collids = ContentCollection.getCollectionIds(orgId,Permission.All,false)

      val cursor = ContentCollection.find(MongoDBObject("_id" -> MongoDBObject("$in" -> collids)))

      val colls = cursor.toSeq
      cursor.close()
      Ok(Json.toJson(colls))
    }else Forbidden(Json.toJson(ApiError.UnauthorizedOrganization))
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
              val collection = ContentCollection(id = newId, name = name.get)
              ContentCollection.insert(request.ctx.organization,collection) match {
                case Right(coll) => Ok(Json.toJson(coll))
                case Left(e) => InternalServerError(Json.toJson(ApiError.InsertCollection(e.clientOutput)))
              }

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
          val toUpdate = ContentCollection( name, id = original.id)
          ContentCollection.updateCollection(toUpdate) match {
            case Right(coll) => (json \ "organizations").asOpt[Seq[String]].map( seq => seq.map( new ObjectId(_)) ) match {
              case Some(orgIds) => ContentCollection.addOrganizations(orgIds, id, Permission.All) match {
                case Right(_) => Ok(Json.toJson(coll))
                case Left(e) => InternalServerError(Json.toJson(ApiError.AddToOrganization(e.clientOutput)))
              }
              case None => Ok(Json.toJson(coll))
            }
            case Left(e) => InternalServerError(Json.toJson(ApiError.UpdateCollection(e.clientOutput)))
          }

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
}
