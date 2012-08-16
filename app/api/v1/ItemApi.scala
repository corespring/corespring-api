package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.Logger
import api.{InvalidFieldException, ApiError, QueryHelper}
import models.{ContentCollection, Content, Item}
import com.mongodb.util.JSONParseException
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import play.api.templates.Xml
import play.api.mvc.Result
import play.api.libs.json.Json


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

  val xmlDataField = MongoDBObject(Item.XmlData -> 1)

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
    val initSearch = MongoDBObject(Content.collId -> MongoDBObject("$in" -> ContentCollection.getCollectionIds(request.ctx.organization,Permission.All)))
    QueryHelper.list(q, f, c, sk, l, Item.queryFields, Item.collection, initSearch)
  }

  /**
   * Returns an Item.  Only the default fields are rendered back.
   *
   * @param id
   * @return
   */
  def getItem(id: ObjectId) = ApiAction { request =>
    _getItem(id, excludedFieldsByDefault)
  }

  /**
   * Returns an Item with all its fields.
   *
   * @param id
   * @return
   */
  def getItemDetail(id: ObjectId) = ApiAction { request =>
    _getItem(id, None)
  }

  /**
   * Helper method to retrieve Items from mongo.
   *
   * @param id
   * @param fields
   * @return
   */
  private def _getItem(id: ObjectId, fields: Option[DBObject]): Result  = {
    fields.map( Item.collection.findOneByID(id, _)).getOrElse( Item.collection.findOneByID(id)) match {
      case Some(o) =>  Ok(com.mongodb.util.JSON.serialize(o))
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
      case Some(o) => {
        Ok(Xml(o.get(Item.XmlData).toString))
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
    // todo: 1) how do we know if we can allow this operation?
    // todo: 2) what else do we need to delete? Probably references to this item in content collections?
    // todo: 3) is it OK to delete an item someone might be using?
    val idField = MongoDBObject("_id" -> id)

    Item.collection.findOneByID(id, idField) match {
      case Some(o) => {
        Item.collection.remove(idField)
        Ok(com.mongodb.util.JSON.serialize(o))
      }
      case _ => NotFound
    }
  }

  def createItem = ApiAction { request =>
    request.body.asJson match {
      case Some(json) => {
        try {
          Logger.info("Item.createItem: received json: %s".format(json))
          val dbObj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
          if ( dbObj.isDefinedAt("id") ) {
            BadRequest(Json.toJson(ApiError.IdNotNeeded))
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
        request.body.asJson match {
          case Some(json) => {
            try {
              Logger.info("Item.updateItem: received json: %s".format(json))

              val dbObj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
              val hasId = dbObj.isDefinedAt("_id")

              if ( hasId && !dbObj.get("_id").equals(id) ) {
                BadRequest(Json.toJson(ApiError.IdsDoNotMatch))
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
      }
      case _ => NotFound
    }
  }
}
