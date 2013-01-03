package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.libs.json._
import models.{ContentCollection, Organization}
import org.bson.types.ObjectId
import api.{QueryHelper, ApiError}
import com.mongodb.casbah.commons.MongoDBObject
import controllers.{InternalError, Utils}
import scala.Left
import play.api.libs.json.JsArray
import scala.Some
import scala.Right

/**
 * The Collections API
 */

object CollectionApi extends BaseApi {
  /**
   * Returns a list of collections visible to the organization in the request context
   *
   * @return
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    doList(request.ctx.organization, q, f, c, sk, l)
  }

  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    if (Organization.isChild(request.ctx.organization, orgId)) {
      doList(orgId, q, f, c, sk, l)
    } else
      Forbidden(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  private def doList(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = {
    val collids = ContentCollection.getCollectionIds(orgId,Permission.Read, false)
    println(collids)
    val initSearch = MongoDBObject("_id" -> MongoDBObject("$in" -> collids))
    QueryHelper.list(q, f, c, sk, l, ContentCollection, Some(initSearch))
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

  private def addCollectionToOrganizations(values:Seq[JsValue], collId:ObjectId):Either[controllers.InternalError,Unit] = {
    val orgs:Seq[(ObjectId,Permission)] = values.map(v => v match {
      case JsString(strval) => (new ObjectId(strval) -> Permission.Read)
      case JsObject(orgWithPerm) => (new ObjectId(orgWithPerm(1)._1) -> Permission.fromLong(orgWithPerm(1)._2.as[Long]).get)
      case _ => return Right(InternalError("incorrect format for organizations",addMessageToClientOutput = true))
    })
    ContentCollection.addOrganizations(orgs,collId)
  }
  /**
   * Updates a collection
   *
   * @return
   */
  def updateCollection(id: ObjectId) = ApiAction { request =>
    ContentCollection.findOneById(id).map( original => {
      request.body.asJson match {
        case Some(json) => {
          val name = (json \ "name").asOpt[String].getOrElse(original.name)
          val toUpdate = ContentCollection( name, id = original.id)
          if((Organization.getPermissions(request.ctx.organization,original.id).value&Permission.Read.value) == Permission.Read.value) {
            ContentCollection.updateCollection(toUpdate) match {
              case Right(coll) => (json \ "organizations") match {
                case JsArray(values) => addCollectionToOrganizations(values,id) match {
                  case Right(_) => Ok(Json.toJson(coll))
                  case Left(e) => InternalServerError(Json.toJson(ApiError.AddToOrganization(e.clientOutput)))
                }
                case JsUndefined(_) => Ok(Json.toJson(coll))
                case _ => BadRequest(Json.toJson(ApiError.UpdateCollection(Some("organizations was included but was not the right format"))))
              }
              case Left(e) => InternalServerError(Json.toJson(ApiError.UpdateCollection(e.clientOutput)))
            }
          }else Unauthorized(Json.toJson(ApiError.UpdateCollection(Some("you do not have permission to update this collection"))))
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
