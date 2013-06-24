package api.v1

import api.ApiError
import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatMongoCursor
import controllers.auth.{Permission, BaseApi}
import controllers.{InternalError, Utils}
import models.search.SearchCancelled
import models.{ContentCollection, Organization}
import play.api.libs.json.JsArray
import play.api.libs.json._
import play.api.mvc.Result
import scala.Left
import scala.Right
import scala.Some

/**
 * The Collections API
 */

object CollectionApi extends BaseApi {
  /**
   * Returns a list of collections visible to the organization in the request context
   *
   * @return
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiActionRead {
    request =>
      doList(request.ctx.organization, q, f, c, sk, l, sort)
  }

  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiActionRead {
    request =>
      if (Organization.getTree(request.ctx.organization).exists(_.id == orgId)) {
        doList(orgId, q, f, c, sk, l, sort)
      } else
        Forbidden(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  private def doList(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, optsort: Option[String]) = {
    val collids = ContentCollection.getCollectionIds(orgId, Permission.Read, true)
    val initSearch = MongoDBObject("_id" -> MongoDBObject("$in" -> collids))
    def applySort(colls: SalatMongoCursor[ContentCollection]): Result = {
      optsort.map(ContentCollection.toSortObj(_)) match {
        case Some(Right(sort)) => Ok(Json.toJson(Utils.toSeq(colls.sort(sort).skip(sk).limit(l))))
        case None => Ok(Json.toJson(Utils.toSeq(colls.skip(sk).limit(l))))
        case Some(Left(error)) => BadRequest(Json.toJson(ApiError.InvalidSort(error.clientOutput)))
      }
    }
    q.map(ContentCollection.toSearchObj(_, Some(initSearch))).getOrElse[Either[SearchCancelled, MongoDBObject]](Right(initSearch)) match {
      case Right(query) => f.map(ContentCollection.toFieldsObj(_)) match {
        case Some(Right(searchFields)) => if (c == "true") Ok(JsObject(Seq("count" -> JsNumber(ContentCollection.find(query).count))))
        else applySort(ContentCollection.find(query, searchFields.dbfields))
        case None => if (c == "true") Ok(JsObject(Seq("count" -> JsNumber(ContentCollection.find(query).count))))
        else applySort(ContentCollection.find(query))
        case Some(Left(error)) => BadRequest(Json.toJson(ApiError.InvalidFields(error.clientOutput)))
      }
      case Left(sc) => sc.error match {
        case None => Ok(JsArray(Seq()))
        case Some(error) => BadRequest(Json.toJson(ApiError.InvalidQuery(error.clientOutput)))
      }
    }
  }

  /**
   * Returns a Collection by its id
   *
   * @param id The collection id
   * @return
   */
  def getCollection(id: ObjectId) = ApiActionRead {
    request =>
      ContentCollection.findOneById(id) match {
        case Some(org) => {
          // todo: check if this collection is visible to the caller?
          Ok(Json.toJson(org))
        }
        case _ => NotFound
      }
  }

  def createCollection = ApiActionWrite {
    request =>
      request.body.asJson match {
        case Some(json) => {
          val newId = new ObjectId
          val name = (json \ "name").asOpt[String]
          if (name.isEmpty) {
            BadRequest(Json.toJson(ApiError.CollectionNameMissing))
          } else {
            val collection = ContentCollection(id = newId, name = name.get)
            ContentCollection.insertCollection(request.ctx.organization, collection, Permission.Write) match {
              case Right(coll) => Ok(Json.toJson(coll))
              case Left(e) => InternalServerError(Json.toJson(ApiError.InsertCollection(e.clientOutput)))
            }
          }
        }
        case _ => jsonExpected
      }
  }

  private def unknownCollection = NotFound(Json.toJson(ApiError.UnknownCollection))

  private def addCollectionToOrganizations(values: Seq[JsValue], collId: ObjectId): Either[controllers.InternalError, Unit] = {
    val orgs: Seq[(ObjectId, Permission)] = values.map(v => v match {
      case JsString(strval) => (new ObjectId(strval) -> Permission.Read)
      case JsObject(orgWithPerm) => (new ObjectId(orgWithPerm(1)._1) -> Permission.fromLong(orgWithPerm(1)._2.as[Long]).get)
      case _ => return Right(InternalError("incorrect format for organizations"))
    })
    ContentCollection.addOrganizations(orgs, collId)
  }

  /**
   * Updates a collection
   *
   * @return
   */
  def updateCollection(id: ObjectId) = ApiActionWrite {
    request =>
      ContentCollection.findOneById(id).map(original => {
        request.body.asJson match {
          case Some(json) => {
            val name = (json \ "name").asOpt[String].getOrElse(original.name)
            val toUpdate = ContentCollection(name, id = original.id)
            if ((Organization.getPermissions(request.ctx.organization, original.id).value & Permission.Read.value) == Permission.Read.value) {
              ContentCollection.updateCollection(toUpdate) match {
                case Right(coll) => (json \ "organizations") match {
                  case JsArray(values) => addCollectionToOrganizations(values, id) match {
                    case Right(_) => Ok(Json.toJson(coll))
                    case Left(e) => InternalServerError(Json.toJson(ApiError.AddToOrganization(e.clientOutput)))
                  }
                  case JsUndefined(_) => Ok(Json.toJson(coll))
                  case _ => BadRequest(Json.toJson(ApiError.UpdateCollection(Some("organizations was included but was not the right format"))))
                }
                case Left(e) => InternalServerError(Json.toJson(ApiError.UpdateCollection(e.clientOutput)))
              }
            } else Unauthorized(Json.toJson(ApiError.UpdateCollection(Some("you do not have permission to update this collection"))))
          }
          case _ => jsonExpected
        }
      }).getOrElse(unknownCollection)
  }

  /**
   * Deletes a collection
   */
  def deleteCollection(id: ObjectId) = ApiActionWrite {
    request =>
      ContentCollection.findOneById(id) match {
        case Some(coll) => ContentCollection.delete(id) match {
          case Right(_) => Ok(Json.toJson(coll))
          case Left(e) => InternalServerError(Json.toJson(ApiError.DeleteCollection(e.clientOutput)))
        }
        case None => BadRequest(Json.toJson(ApiError.DeleteCollection))
      }
  }
}
