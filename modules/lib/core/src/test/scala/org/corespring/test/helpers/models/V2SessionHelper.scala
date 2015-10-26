package org.corespring.test.helpers.models

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{ Item, DynamoDB }
import com.amazonaws.services.dynamodbv2.model.{ QueryRequest, AttributeValue }
import org.bson.types.ObjectId
import org.corespring.common.aws.AwsUtil
import org.corespring.common.config.AppConfig
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.sessiondb.SessionDbConfig
import play.api.Play
import play.api.libs.json.{ JsObject, Json, JsValue }
import se.radley.plugin.salat.SalatPlugin
import com.mongodb.casbah.Imports._

trait V2SessionHelper {

  def create(itemId: VersionedId[ObjectId], tableName: String, orgId: Option[ObjectId] = None): ObjectId
  def update(sessionId: ObjectId, json: JsValue, tableName: String): Unit
  def findSessionForItemId(itemId: VersionedId[ObjectId], tableName: String): ObjectId
  def findSession(sessionId: String, tableName: String): Option[JsObject]
  def delete(sessionId: ObjectId, tableName: String): Unit

}

class V2DynamoSessionHelper extends V2SessionHelper {

  lazy val dbClient = AwsUtil.dynamoDbClient()

  lazy val db = new DynamoDB(dbClient)

  def create(itemId: VersionedId[ObjectId], tableName: String, orgId: Option[ObjectId] = None): ObjectId = {
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

  def update(sessionId: ObjectId, json: JsValue, tableName: String): Unit = {
    findSession(sessionId.toString, tableName) match {
      case js: Option[JsObject] =>
        val newJson = js.get ++ json.as[JsObject]
        val itemId = (newJson \ "itemId").as[String]
        db.getTable(tableName).putItem(new Item()
          .withPrimaryKey("id", sessionId.toString)
          .withString("itemId", itemId)
          .withJSON("json", newJson.toString))
    }
  }

  def findSessionForItemId(itemId: VersionedId[ObjectId], tableName: String): ObjectId = {

    def expressionAttributeValues = {
      import scala.collection.JavaConverters._
      Seq(":itemId" -> new AttributeValue(itemId.toString)).toMap.asJava
    }

    val req = new QueryRequest()
      .withTableName(tableName)
      .withIndexName("itemId-index")
      .withKeyConditionExpression("itemId = :itemId")
      .withExpressionAttributeValues(expressionAttributeValues)

    val res = dbClient.query(req).getItems()

    if (res.size() == 0) {
      throw new RuntimeException(s"Can't find session for item id: $itemId in table: $tableName")
    }

    val item = res.get(0)
    new ObjectId(item.get("id").getS)
  }

  def findSession(sessionId: String, tableName: String): Option[JsObject] = {
    db.getTable(tableName).getItem("id", sessionId) match {
      case item: Item => Some(Json.parse(item.getJSON("json").toString).as[JsObject])
      case _ => None
    }
  }

  def delete(sessionId: ObjectId, tableName: String): Unit = {
    db.getTable(tableName).deleteItem("id", sessionId.toString)
  }

}

class V2MongoSessionHelper extends V2SessionHelper {

  import scala.language.implicitConversions

  private implicit def strToObjectId(id: String) = new ObjectId(id)

  lazy val db = {
    Play.current.plugin[SalatPlugin].map {
      p =>
        p.db()
    }.getOrElse(throw new RuntimeException("Error loading salat plugin"))
  }

  def create(itemId: VersionedId[ObjectId], tableName: String, orgId: Option[ObjectId] = None): ObjectId = {
    val oid = ObjectId.get
    val baseSession = idQuery(oid) ++ MongoDBObject("itemId" -> itemId.toString)
    val session = orgId match {
      case Some(orgId) => baseSession ++ MongoDBObject("identity" -> MongoDBObject("orgId" -> orgId.toString))
      case _ => baseSession
    }
    db(tableName).insert(session)
    oid
  }

  def update(sessionId: ObjectId, json: JsValue, tableName: String): Unit = {
    val dbo = com.mongodb.util.JSON.parse(Json.stringify(json)).asInstanceOf[DBObject]
    db(tableName).update(idQuery(sessionId), dbo)
  }

  def findSessionForItemId(itemId: VersionedId[ObjectId], tableName: String): ObjectId = {
    db(tableName).findOne(MongoDBObject("itemId" -> itemId.toString()))
      .map(o => o.get("_id").asInstanceOf[ObjectId])
      .getOrElse(throw new RuntimeException(s"Can't find session for item id: $itemId"))
  }

  def findSession(sessionId: String, tableName: String): Option[JsObject] = {
    db(tableName).findOne(idQuery(sessionId))
      .map(o => Some(Json.parse(o.toString).as[JsObject]))
      .getOrElse(throw new RuntimeException(s"Can't find session with id: $sessionId"))
  }

  def delete(sessionId: ObjectId, tableName: String): Unit = {
    db(tableName).remove(MongoDBObject("_id" -> sessionId))
  }

  private def idQuery(id: ObjectId) = MongoDBObject("_id" -> id)
}

object V2SessionHelper extends V2SessionHelper {

  val v2ItemSessions = SessionDbConfig.sessionTable
  val v2ItemSessionsPreview = SessionDbConfig.previewSessionTable

  lazy val impl = {
    if (AppConfig.dynamoDbActivate) {
      new V2DynamoSessionHelper
    } else {
      new V2MongoSessionHelper
    }
  }

  def create(itemId: VersionedId[ObjectId], tableName: String = v2ItemSessions, orgId: Option[ObjectId] = None): ObjectId = {
    impl.create(itemId, tableName, orgId)
  }
  def update(sessionId: ObjectId, json: JsValue, tableName: String = v2ItemSessions): Unit = {
    impl.update(sessionId, json, tableName)
  }
  def findSessionForItemId(itemId: VersionedId[ObjectId], tableName: String = v2ItemSessions): ObjectId = {
    impl.findSessionForItemId(itemId, tableName)
  }
  def findSession(sessionId: String, tableName: String = v2ItemSessions): Option[JsObject] = {
    impl.findSession(sessionId, tableName)
  }
  def delete(sessionId: ObjectId, tableName: String = v2ItemSessions): Unit = {
    impl.delete(sessionId, tableName)
  }
}

