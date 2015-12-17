package org.corespring.it.helpers

import com.amazonaws.services.dynamodbv2.document.{ DynamoDB, Item }
import com.amazonaws.services.dynamodbv2.model.{ AttributeValue, QueryRequest }
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.sessiondb.SessionDbConfig
import play.api.Play
import play.api.libs.json.{ JsObject, JsValue, Json }
import se.radley.plugin.salat.SalatPlugin

trait V2SessionHelper {
  def create(itemId: VersionedId[ObjectId], orgId: Option[ObjectId] = None): ObjectId
  def update(sessionId: ObjectId, json: JsValue): Unit
  def findSessionForItemId(itemId: VersionedId[ObjectId]): ObjectId
  def findSession(sessionId: String): Option[JsObject]
  def delete(sessionId: ObjectId): Unit
}

object V2SessionHelper {

  def apply(config: SessionDbConfig, previewTable: Boolean = false): V2SessionHelper = {
    val tableName = if (previewTable) config.previewSessionTable else config.sessionTable
    if (config.useDynamo) {
      new V2DynamoSessionHelper(tableName)
    } else {
      new V2MongoSessionHelper(tableName)
    }
  }
}

private class V2DynamoSessionHelper(tableName: String) extends V2SessionHelper {

  lazy val dbClient = global.Global.main.dbClient

  lazy val db = new DynamoDB(dbClient)

  override def create(itemId: VersionedId[ObjectId], orgId: Option[ObjectId] = None): ObjectId = {
    val oid = ObjectId.get
    val baseSession = Json.obj("id" -> oid.toString, "itemId" -> itemId.toString)
    val session = orgId match {
      case Some(orgId) => baseSession ++ Json.obj("identity" -> Json.obj("orgId" -> orgId.toString))
      case _ => baseSession
    }
    db.getTable(tableName).putItem(new Item()
      .withPrimaryKey("id", oid.toString)
      .withString("itemId", itemId.toString)
      .withJSON("json", session.toString))
    oid
  }

  override def update(sessionId: ObjectId, json: JsValue): Unit = {
    findSession(sessionId.toString) match {
      case js: Option[JsObject] =>
        val newJson = js.get ++ json.as[JsObject]
        val itemId = (newJson \ "itemId").as[String]
        db.getTable(tableName).putItem(new Item()
          .withPrimaryKey("id", sessionId.toString)
          .withString("itemId", itemId)
          .withJSON("json", newJson.toString))
    }
  }

  override def findSessionForItemId(itemId: VersionedId[ObjectId]): ObjectId = {

    def expressionAttributeValues = {
      import scala.collection.JavaConverters._
      Seq(":itemId" -> new AttributeValue(itemId.toString)).toMap.asJava
    }

    val req = new QueryRequest()
      .withTableName(tableName)
      .withIndexName("itemId-index")
      .withKeyConditionExpression("itemId = :itemId")
      .withExpressionAttributeValues(expressionAttributeValues)

    val res = dbClient.query(req).getItems

    if (res.size() == 0) {
      throw new RuntimeException(s"Can't find session for item id: $itemId in table: $tableName")
    }

    val item = res.get(0)
    new ObjectId(item.get("id").getS)
  }

  override def findSession(sessionId: String): Option[JsObject] = {
    db.getTable(tableName).getItem("id", sessionId) match {
      case item: Item => Some(Json.parse(item.getJSON("json")).as[JsObject])
      case _ => None
    }
  }

  override def delete(sessionId: ObjectId): Unit = {
    db.getTable(tableName).deleteItem("id", sessionId.toString)
  }

}

private class V2MongoSessionHelper(tableName: String) extends V2SessionHelper {

  import scala.language.implicitConversions

  private implicit def strToObjectId(id: String): ObjectId = new ObjectId(id)

  lazy val db = {
    Play.current.plugin[SalatPlugin].map {
      p =>
        p.db()
    }.getOrElse(throw new RuntimeException("Error loading salat plugin"))
  }

  override def create(itemId: VersionedId[ObjectId], orgId: Option[ObjectId] = None): ObjectId = {
    val oid = ObjectId.get
    val baseSession = idQuery(oid) ++ MongoDBObject("itemId" -> itemId.toString)
    val session = orgId match {
      case Some(o) => baseSession ++ MongoDBObject("identity" -> MongoDBObject("orgId" -> o.toString))
      case _ => baseSession
    }
    db(tableName).insert(session)
    oid
  }

  override def update(sessionId: ObjectId, json: JsValue): Unit = {
    val dbo = com.mongodb.util.JSON.parse(Json.stringify(json)).asInstanceOf[DBObject]
    db(tableName).update(idQuery(sessionId), dbo)
  }

  override def findSessionForItemId(itemId: VersionedId[ObjectId]): ObjectId = {
    db(tableName).findOne(MongoDBObject("itemId" -> itemId.toString()))
      .map(o => o.get("_id").asInstanceOf[ObjectId])
      .getOrElse(throw new RuntimeException(s"Can't find session for item id: $itemId"))
  }

  override def findSession(sessionId: String): Option[JsObject] = {
    db(tableName).findOne(idQuery(sessionId))
      .map(o => Some(Json.parse(o.toString).as[JsObject]))
      .getOrElse(throw new RuntimeException(s"Can't find session with id: $sessionId"))
  }

  override def delete(sessionId: ObjectId): Unit = {
    db(tableName).remove(MongoDBObject("_id" -> sessionId))
  }

  private def idQuery(id: ObjectId) = MongoDBObject("_id" -> id)
}

