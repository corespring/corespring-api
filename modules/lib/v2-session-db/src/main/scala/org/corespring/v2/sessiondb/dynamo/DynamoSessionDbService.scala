package org.corespring.v2.sessiondb.dynamo

import com.amazonaws.services.dynamodbv2.document.{Item, Table}
import org.corespring.v2.sessiondb.SessionDbService
import org.bson.types.ObjectId
import play.api.libs.json.{Json, JsValue}


/**
 * Writes/Reads session to db as (sessionId,itemId,json)
 */
class DynamoSessionDbService(table: Table) extends SessionDbService {

  val itemIdKey = "itemId"
  val jsonKey = "json"
  val sessionIdKey = "id"

  override def create(data: JsValue): Option[ObjectId] = {
    val sessionId = (data \ sessionIdKey).asOpt[String].getOrElse((new ObjectId).toString)
    val itemId = (data \ itemIdKey).asOpt[String].getOrElse("")
    val item = mkItem(sessionId,itemId,data)
    table.putItem(item)
    Some(new ObjectId(sessionId))
  }

  override def load(sessionId: String): Option[JsValue] = {
    table.getItem(sessionIdKey, sessionId) match {
      case item:Item => {
        Some(Json.parse(item.getJSON(jsonKey)))
      }
      case _ => None
    }
  }

  override def save(sessionId: String, data: JsValue): Option[JsValue] = {
    val itemId = (data \ itemIdKey).asOpt[String].getOrElse("")
    val item = mkItem(sessionId,itemId,data)
    table.putItem(item)
    Some(data)
  }

  private def mkItem(sessionId:String, itemId:String, json:JsValue) = {
    val item = new Item().withPrimaryKey(sessionIdKey, sessionId)
    if(!itemId.isEmpty) {
      item.withString(itemIdKey, itemId)
    }
    if(!json.toString.isEmpty) {
      item.withJSON(jsonKey, json.toString)
    }
    item
  }

}
