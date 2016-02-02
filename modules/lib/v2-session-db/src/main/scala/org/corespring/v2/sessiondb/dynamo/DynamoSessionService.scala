package org.corespring.v2.sessiondb.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{ Item, Table }
import com.amazonaws.services.dynamodbv2.model._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.sessiondb.SessionService
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json.{ Json, JsValue }

import scala.collection.JavaConversions._
import scalaz.Validation

/**
 * Writes/Reads session to db as (sessionId,itemId,json)
 */
class DynamoSessionService(table: Table, client: AmazonDynamoDBClient) extends SessionService {

  val itemIdKey = "itemId"
  val jsonKey = "json"
  val sessionIdKey = "id"

  override def sessionCount(itemId: VersionedId[ObjectId]): Long = {
    val request = new QueryRequest()
      .withTableName(table.getTableName)
      .withIndexName("itemId-index")
      .withExclusiveStartKey(null)
      .withKeyConditions(Map("itemId" -> new Condition()
      .withComparisonOperator(ComparisonOperator.EQ.toString)
      .withAttributeValueList(new AttributeValue().withS(itemId.toString))
    ))

    val r = client.query(request)
    r.getCount.toLong
  }

  def create(data: JsValue): Option[ObjectId] = {
    val sessionId = (data \ sessionIdKey).asOpt[String].getOrElse((new ObjectId).toString)
    val itemId = (data \ itemIdKey).asOpt[String].getOrElse("")
    val item = mkItem(sessionId, itemId, data)
    table.putItem(item)
    Some(new ObjectId(sessionId))
  }

  def load(sessionId: String): Option[JsValue] = {
    table.getItem(sessionIdKey, sessionId) match {
      case item: Item => {
        Some(Json.parse(item.getJSON(jsonKey)))
      }
      case _ => None
    }
  }

  def save(sessionId: String, data: JsValue): Option[JsValue] = {
    val itemId = (data \ itemIdKey).asOpt[String].getOrElse("")
    val item = mkItem(sessionId, itemId, data)
    table.putItem(item)
    Some(data)
  }

  private def mkItem(sessionId: String, itemId: String, json: JsValue) = {
    val item = new Item().withPrimaryKey(sessionIdKey, sessionId)
    if (!itemId.isEmpty) {
      item.withString(itemIdKey, itemId)
    }
    if (!json.toString.isEmpty) {
      item.withJSON(jsonKey, json.toString)
    }
    item
  }

  override def orgCount(orgId: ObjectId, mount: DateTime): Option[Map[DateTime, Long]] = ???

}
