package api.v1

import controllers.auth.BaseApi
import play.api.Logger
import api.{InvalidFieldException, ApiError, CountResult, QueryHelper}
import models.Item
import play.api.libs.json.Json
import com.mongodb.util.JSONParseException
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import play.api.templates.Xml
import play.api.mvc.Result


/**
 * Items API
 */

object ItemApi extends BaseApi {

  //todo: have evaneus define what should be returned by default for /items/:id
  val defaultFields = Some(MongoDBObject(
    "_id" -> 1,
    "title" -> 1,
    "author" -> 1
  ))

  val xmlDataField = MongoDBObject("xmlData" -> 1)

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
    Logger.debug("ItemAPI: q in controller = %s".format(q))

    //todo: this block of code is going to be very similar with all the operations supporting list queries.
    // I need to refactor this so the common code is reused among all of them.
    try {
      val query = q.map( QueryHelper.parse(_, Item.queryFields) ).getOrElse( new MongoDBObject() )
      val cursor = Item.collection.find(query)
      cursor.skip(sk)
      cursor.limit(l)

      // I'm using a String for c because if I use a boolean I need to pass 0 or 1 from the command line for Play to parse the boolean.
      // I think using "true" or "false" is better
      if ( c.equalsIgnoreCase("true") )
        Ok(CountResult.toJson(cursor.count))
      else
        Ok(com.mongodb.util.JSON.serialize(cursor.toList))
    } catch {
      case e: JSONParseException => BadRequest(Json.toJson(ApiError.InvalidQuery))
      case ife: InvalidFieldException => BadRequest(Json.toJson(ApiError.UnknownFieldOrOperator.format(ife.field)))
    }
  }

  /**
   * Returns an Item.  Only the default fields are rendered back.
   *
   * @param id
   * @return
   */
  def getItem(id: ObjectId) = ApiAction { request =>
    _getItem(id, defaultFields)
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
        Ok(Xml(o.get("xmlData").toString))
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
}
