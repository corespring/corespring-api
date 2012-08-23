package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.Logger
import api.{InvalidFieldException, ApiError, QueryHelper}
import models.{Organization, ContentCollection, Content, Item}
import com.mongodb.util.JSONParseException
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import play.api.templates.Xml
import play.api.mvc.Result
import play.api.libs.json.Json
import com.typesafe.config.ConfigFactory


/**
 * Items API
 */

object ItemApi extends BaseApi {

  //todo: confirm with evaneus this is what should be returned by default for /items/:id
  val excludedFieldsByDefault = Some(MongoDBObject(
    Item.CopyrightOwner -> 0,
    Item.Credentials -> 0,
    Item.Files -> 0,
    Item.KeySkills -> 0,
    Item.ContentType -> 0,
    Item.XmlData -> 0
  ))

  val xmlDataField = MongoDBObject(Item.XmlData -> 1, Item.CollectionId -> 1)

  /**
   * List query implementation for Items
   *
   * @param q
   * @param f
   * @param c
   * @param sk
   * @param l
   * @return
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    doList(request.ctx.organization, q, f, c, sk, l)
  }

  def listWithOrg(orgId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    if (Organization.isChild(request.ctx.organization,orgId)) {
      doList(orgId, q, f, c, sk, l)
    } else Forbidden(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  def listWithColl(collId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    if (ContentCollection.isAuthorized(request.ctx.organization,collId,Permission.All)) {
      val initSearch = MongoDBObject(Content.collectionId -> collId.toString)
      QueryHelper.list(q, f, c, sk, l, Item.queryFields, Item.collection, Some(initSearch))
    } else Forbidden(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  private def doList(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = {
    val initSearch = MongoDBObject(Content.collectionId -> MongoDBObject("$in" -> ContentCollection.getCollectionIds(orgId,Permission.All).map(_.toString)))
    QueryHelper.list(q, f, c, sk, l, Item.queryFields, Item.collection, Some(initSearch))
  }

  /**
   * Returns an Item.  Only the default fields are rendered back.
   *
   * @param id
   * @return
   */
  def getItem(id: ObjectId) = ApiAction { request =>
    _getItem(request.ctx.organization, id, excludedFieldsByDefault)
  }

  /**
   * Returns an Item with all its fields.
   *
   * @param id
   * @return
   */
  def getItemDetail(id: ObjectId) = ApiAction { request =>
    _getItem(request.ctx.organization, id, None)
  }

  /**
   * Helper method to retrieve Items from mongo.
   *
   * @param id
   * @param fields
   * @return
   */
  private def _getItem(callerOrg: ObjectId, id: ObjectId, fields: Option[DBObject]): Result  = {
    fields.map( Item.collection.findOneByID(id, _)).getOrElse( Item.collection.findOneByID(id)) match {
      case Some(o) =>  if ( canUpdateOrDelete(callerOrg, o.get(Item.CollectionId).asInstanceOf[String])) {
        Ok(com.mongodb.util.JSON.serialize(o))
      } else {
        Forbidden
      }
      case _ => NotFound
    }
  }

  /**
   * Returns the raw content body for the item
   *
   * @param id
   * @return
   */
  def getItemData(id: ObjectId) = ApiAction { request =>
    Item.collection.findOneByID(id, xmlDataField) match {
      case Some(o) =>  if ( canUpdateOrDelete(request.ctx.organization, o.get(Item.CollectionId).asInstanceOf[String])) {
        Ok(Xml(o.get(Item.XmlData).toString))
      } else {
        Forbidden
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
  def deleteItem(id: ObjectId) = ApiAction { request =>
    val idField = MongoDBObject("_id" -> id)

    Item.collection.findOneByID(id, idField) match {
      case Some(o) => {
        if ( canUpdateOrDelete(request.ctx.organization, o.get(Item.CollectionId).asInstanceOf[String]) ) {
          Item.collection.remove(idField)
          Ok(com.mongodb.util.JSON.serialize(o))
        } else {
          Forbidden
        }
      }
      case _ => NotFound
    }
  }

  def createItem = ApiAction { request =>
    request.body.asJson match {
      case Some(json) => {
        try {
          Logger.debug("Item.createItem: received json: %s".format(json))
          val dbObj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
          if ( dbObj.isDefinedAt("_id") ) {
            BadRequest(Json.toJson(ApiError.IdNotNeeded))
          } else if ( !dbObj.isDefinedAt(Item.CollectionId) ) {
            BadRequest(Json.toJson(ApiError.CollectionIsRequired))
          } else {
            QueryHelper.validateFields(dbObj, Item.queryFields)
            val toSave = dbObj += ("_id" -> new ObjectId())
            val wr = Item.collection.insert(toSave)
            val commandResult = wr.getCachedLastError
            if ( commandResult == null || commandResult.ok() ) {
              Ok(com.mongodb.util.JSON.serialize(dbObj)).as(JSON)
            } else {
              Logger.error("There was an error inserting an item: %s".format(commandResult.toString))
              InternalServerError(Json.toJson(ApiError.CantSave))
            }
          }
        } catch {
          case parseEx: JSONParseException => BadRequest(Json.toJson(ApiError.JsonExpected))
          case invalidField: InvalidFieldException => BadRequest(Json.toJson(ApiError.InvalidField.format(invalidField.field)))
        }
      }
      case _ => BadRequest(Json.toJson(ApiError.JsonExpected))
    }
  }

  def updateItem(id: ObjectId) = ApiAction { request =>
    Item.collection.findOneByID(id) match {
      case Some(item) => {
        if ( canUpdateOrDelete(request.ctx.organization, item.get(Item.CollectionId).asInstanceOf[String]) ) {
          request.body.asJson match {
            case Some(json) => {
              try {
                Logger.debug("Item.updateItem: received json: %s".format(json))

                val dbObj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
                val hasId = dbObj.isDefinedAt("_id")

                if ( hasId && !dbObj.get("_id").equals(id) ) {
                  BadRequest(Json.toJson(ApiError.IdsDoNotMatch))
                } else if ( !dbObj.isDefinedAt(Item.CollectionId) ) {
                  BadRequest(Json.toJson(ApiError.CollectionIsRequired))
                } else {
                  QueryHelper.validateFields(dbObj, Item.queryFields)
                  val toSave = item ++ dbObj
                  val wr = Item.collection.update(MongoDBObject("_id" -> id), toSave)
                  val commandResult = wr.getCachedLastError
                  if ( commandResult == null || commandResult.ok() ) {
                    Ok(com.mongodb.util.JSON.serialize(toSave)).as(JSON)
                  } else {
                    Logger.error("There was an error updating item %s: %s".format(id, commandResult.toString))
                    InternalServerError(Json.toJson(ApiError.CantSave))
                  }
                }
              } catch {
                case parseEx: JSONParseException => BadRequest(Json.toJson(ApiError.JsonExpected))
                case invalidField: InvalidFieldException => BadRequest(Json.toJson(ApiError.InvalidField.format(invalidField.field)))
              }
            }
            case _ => BadRequest(Json.toJson(ApiError.JsonExpected))
          }
        } else {
          Forbidden
        }
      }
      case _ => NotFound
    }
  }

  private def canUpdateOrDelete(callerOrg: ObjectId, itemCollId: String):Boolean = {
    val ids = ContentCollection.getCollectionIds(callerOrg,Permission.All)
    ids.contains(itemCollId)
  }


  def getItemsInCollection(collId: ObjectId) = ApiAction { request =>
    NotImplemented
  }

  //private final val AMAZON_ASSETS_BUCKET : String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  def getFile( itemId : ObjectId, fileName : String ) = ApiAction { request =>
    //S3Service.s3download(AMAZON_ASSETS_BUCKET, itemId, fileName)
    NotImplemented
  }
}
