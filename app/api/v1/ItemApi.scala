package api.v1

import controllers.auth.{Permission, BaseApi}
import api.{ApiError, QueryHelper}
import item.QueryCleaner
import models._
import com.mongodb.util.JSONParseException
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import com.novus.salat._
import dao.SalatInsertError
import play.api.templates.Xml
import play.api.mvc.{AnyContent, Result}
import play.api.libs.json.Json._
import models.mongoContext._
import scala.Left
import scala.Some
import scala.Right
import api.InvalidFieldException
import controllers.JsonValidationException
import play.api.libs.json.{JsString, Json, JsValue}

/**
 * Items API
 */

object ItemApi extends BaseApi {

  val excludedFieldsByDefault = Some(MongoDBObject(
    Item.copyrightOwner -> 0,
    Item.credentials -> 0,
    Item.contentType -> 0
  ))

  val dataField = MongoDBObject(Item.data -> 1, Item.collectionId -> 1)
  val excludeData = Some(MongoDBObject(Item.data -> 0))

  /**
   * List query implementation for Items
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction {
    request =>
      val query = QueryCleaner.clean(q, request.ctx.organization)

      if ("true".equalsIgnoreCase(c)) {
        Ok(toJson(Item.countItems(Some(query), f)))
      } else {
        val result = Item.list(Some(query), f, sk, l)
        Ok(toJson(result))
      }
  }



  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction {
    request =>
      if (Organization.isChild(request.ctx.organization, orgId)) {
        listWithFields(orgId, q, f, c, sk, l)
      } else Forbidden(toJson(ApiError.UnauthorizedOrganization))
  }

  def listWithColl(collId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction {
    request => listWithCollection(request, collId, q, f, c, sk, l)
  }

  private def listWithCollection(request: ApiRequest[AnyContent], collectionId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int): Result = {
    if (ContentCollection.isAuthorized(request.ctx.organization, collectionId, Permission.All)) {
      val initSearch = MongoDBObject(Content.collectionId -> collectionId.toString)
      QueryHelper.list(q, f, c, sk, l, Item, Some(initSearch))
    } else Forbidden(toJson(ApiError.UnauthorizedOrganization))
  }

  private def listWithFields(orgId: ObjectId, q: Option[String], f: Option[Object], c: String, sk: Int, l: Int) = {
    val initSearch = MongoDBObject(Content.collectionId -> MongoDBObject("$in" -> ContentCollection.getCollectionIds(orgId, Permission.All).map(_.toString)))
    QueryHelper.list(q, f, c, sk, l, Item, Some(initSearch))
  }

  /**
   * Returns an Item.  Only the default fields are rendered back.
   */
  def get(id: ObjectId) = ApiAction {
    request =>
      getWithFields(request.ctx.organization, id, excludedFieldsByDefault)
  }

  /**
   * Returns an Item with all its fields.
   *
   * @param id
   * @return
   */
  def getDetail(id: ObjectId) = ApiAction {
    request =>
      getWithFields(request.ctx.organization, id, excludeData)
  }

  /**
   * Helper method to retrieve Items from mongo.
   *
   * @param id
   * @param fields
   * @return
   */
  private def getWithFields(callerOrg: ObjectId, id: ObjectId, fields: Option[DBObject]): Result = {
    fields.map(Item.collection.findOneByID(id, _)).getOrElse(Item.collection.findOneByID(id)) match {
      case Some(o) => o.get(Item.collectionId) match {
        case collId: String => if (Content.isCollectionAuthorized(callerOrg, collId, Permission.All)) {
          val i = grater[Item].asObject(o)
          Ok(toJson(i))
        } else {
          Forbidden
        }
        case _ => Forbidden
      }
      case _ => NotFound("Item not found: " + id.toString)
    }
  }

  /**
   * Returns the raw content body for the item
   *
   * @param id
   * @return
   */
  def getData(id: ObjectId) = ApiAction {
    request =>
      Item.collection.findOneByID(id, dataField) match {
        case Some(o) => o.get(Item.collectionId) match {
          case collId: String => if (Content.isCollectionAuthorized(request.ctx.organization, collId, Permission.All)) {
            // added this to prevent a NPE when the data is not available in the item
            // this is temporary until bleezmo finishes working on this operation
            if (o.contains(Item.data))
              Ok(Xml(o.get(Item.data).toString))
            else
              Ok("")
          } else {
            Forbidden
          }
          case _ => Forbidden
        }
        case _ => NotFound
      }
  }

  /**
   * Deletes the item matching the id specified
   *
   * @param id
   * @return
   */
  def delete(id: ObjectId) = ApiAction {
    request =>
      Item.collection.findOneByID(id, MongoDBObject(Item.collectionId -> 1)) match {
        case Some(o) => o.get(Item.collectionId) match {
          case collId: String => if (Content.isCollectionAuthorized(request.ctx.organization, collId, Permission.All)) {
            Content.moveToArchive(id) match {
              case Right(_) => Ok(com.mongodb.util.JSON.serialize(o))
              case Left(error) => InternalServerError(toJson(ApiError.Item.Delete(error.clientOutput)))
            }
          } else {
            Forbidden
          }
          case _ => Forbidden
        }
        case _ => NotFound
      }
  }

  /**
   * Note: Have to call this 'cloneItem' instead of 'clone' as clone is a default
   * function.
   * @param id
   * @return
   */
  def cloneItem(id: ObjectId) = ApiAction {
    request =>
      findAndCheckAuthorization(request.ctx.organization, id) match {
        case Left(e) => BadRequest(toJson(e))
        case Right(item) => {
          Item.cloneItem(item) match {
            case Some(clonedItem) => Ok(toJson(clonedItem))
            case _ => BadRequest(toJson(ApiError.Item.Clone))
          }
        }
      }
  }


  private def findAndCheckAuthorization(orgId: ObjectId, id: ObjectId): Either[ApiError, Item] = Item.findOneById(id) match {
    case Some(s) => Content.isCollectionAuthorized(orgId, s.collectionId, Permission.All) match {
      case true => Right(s)
      case false => Left(ApiError.CollectionUnauthorized)
    }
    case None => Left(ApiError.Item.NotFound)
  }

  def create = ApiAction {
    request =>
      request.body.asJson match {
        case Some(json) => {
          try {
            if ((json \ "id").asOpt[String].isDefined) {
              BadRequest(toJson(ApiError.IdNotNeeded))
            } else {
              val i: Item = fromJson[Item](json)
              if (i.collectionId.isEmpty) {
                Organization.getDefaultCollection(request.ctx.organization) match {
                  case Right(default) => {
                    i.collectionId = default.id.toString;
                    Item.insert(i) match {
                      case Some(_) => Ok(toJson(i))
                      case None => InternalServerError(toJson(ApiError.CantSave))
                    }
                  }
                  case Left(error) => InternalServerError(toJson(ApiError.CantSave(error.clientOutput)))
                }
              } else if (Content.isCollectionAuthorized(request.ctx.organization, i.collectionId, Permission.All)) {
                Item.insert(i) match {
                  case Some(_) => Ok(toJson(i))
                  case None => InternalServerError(toJson(ApiError.CantSave))
                }
              } else {
                Forbidden(toJson(ApiError.CollectionUnauthorized))
              }
            }
          } catch {
            case parseEx: JSONParseException => BadRequest(toJson(ApiError.JsonExpected))
            case invalidField: InvalidFieldException => BadRequest(toJson(ApiError.InvalidField.format(invalidField.field)))
            case e: SalatInsertError => InternalServerError(toJson(ApiError.CantSave))
          }
        }
        case _ => BadRequest(toJson(ApiError.JsonExpected))
      }
  }

  def update(id: ObjectId) = ApiAction {
    request =>
      if (Content.isAuthorized(request.ctx.organization, id, Permission.All)) {
        request.body.asJson match {
          case Some(json) => {
            if ((json \ Item.id).asOpt[String].isDefined) {
              BadRequest(toJson(ApiError.IdNotNeeded))
            } else {
              try {
                val item = fromJson[Item](json)
                Item.updateItem(id, item, excludedFieldsByDefault, request.ctx.organization) match {
                  case Right(i) => Ok(toJson(i))
                  case Left(error) => InternalServerError(toJson(ApiError.Item.Update(error.clientOutput)))
                }
              } catch {
                case e: JSONParseException => BadRequest(toJson(ApiError.JsonExpected))
                case e: JsonValidationException => BadRequest(toJson(ApiError.JsonExpected(Some(e.getMessage))))
              }
            }
          }
          case _ => BadRequest(toJson(ApiError.JsonExpected))
        }
      } else Forbidden
  }

  def getItemsInCollection(collId: ObjectId) = ApiAction {
    request =>
      NotImplemented
  }
}
