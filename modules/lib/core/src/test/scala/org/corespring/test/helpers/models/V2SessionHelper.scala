package org.corespring.test.helpers.models

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{ Item, DynamoDB }
import com.amazonaws.services.dynamodbv2.model.{ QueryRequest, AttributeValue }
import org.bson.types.ObjectId
import org.corespring.common.config.{ SessionDbConfig, AppConfig }
import org.corespring.platform.core.models.itemSession.SessionData
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play
import play.api.libs.json.{ JsObject, Json, JsValue }
import se.radley.plugin.salat.SalatPlugin
import com.mongodb.casbah.Imports._

trait V2SessionHelper {

  def create(itemId: VersionedId[ObjectId], name: String, orgId: Option[ObjectId] = None): ObjectId
  def update(sessionId: ObjectId, json: JsValue, name: String): Unit
  def findSessionForItemId(vid: VersionedId[ObjectId], name: String): ObjectId
  def findSession(id: String, name: String): Option[JsObject]
  def delete(sessionId: ObjectId, name: String): Unit

}

class V2DynamoSessionHelper extends V2SessionHelper {

  import scala.language.implicitConversions

  private implicit def strToObjectId(id: String) = new ObjectId(id)

  lazy val dbClient = {
    val client = new AmazonDynamoDBClient(new ProfileCredentialsProvider {
      val configuration = Play.current.configuration
      def getAWSAccessKeyId: String = AppConfig.amazonKey
      def getAWSSecretKey: String = AppConfig.amazonSecret
    })
    if (AppConfig.dynamoDbUseLocal) {
      client.setEndpoint(AppConfig.dynamoDbLocalUrl)
    }
    client
  }

  lazy val db = new DynamoDB(dbClient)

  def create(itemId: VersionedId[ObjectId], name: String, orgId: Option[ObjectId] = None): ObjectId = {
    val oid = ObjectId.get
    val baseSession = idQuery(oid) ++ Json.obj("itemId" -> itemId.toString)
    val session = orgId match {
      case Some(orgId) => baseSession ++ Json.obj("identity" -> Json.obj("orgId" -> orgId.toString))
      case _ => baseSession
    }
    db.getTable(name).putItem(new Item()
      .withPrimaryKey("id", oid.toString)
      .withString("itemId", itemId.toString)
      .withJSON("json", session.toString))
    oid
  }

  def update(sessionId: ObjectId, json: JsValue, name: String): Unit = {
    findSession(sessionId.toString, name) match {
      case js: Option[JsObject] =>
        val newJson = js.get ++ json.as[JsObject]
        val itemId = (newJson \ "itemId").as[String]
        db.getTable(name).putItem(new Item()
          .withPrimaryKey("id", sessionId.toString)
          .withString("itemId", itemId)
          .withJSON("json", newJson.toString))
    }
  }

  def findSessionForItemId(vid: VersionedId[ObjectId], name: String): ObjectId = {

    def toExpressionAttributeValues = {
      import scala.collection.JavaConverters._
      Seq(":itemId" -> new AttributeValue(vid.toString)).toMap.asJava
    }

    val req = new QueryRequest()
      .withTableName(name)
      .withIndexName("itemId-index")
      .withKeyConditionExpression("itemId = :itemId")
      .withExpressionAttributeValues(toExpressionAttributeValues)

    val res = dbClient.query(req).getItems()

    if (res.size() == 0) {
      throw new RuntimeException(s"Can't find session for item id: $vid in table: $name")
    }

    val item = res.get(0)
    new ObjectId(item.get("id").getS)
  }

  def findSession(id: String, name: String): Option[JsObject] = {
    db.getTable(name).getItem("id", id) match {
      case item: Item => Some(Json.parse(item.getJSON("json").toString).as[JsObject])
      case _ => None
    }
  }

  def delete(sessionId: ObjectId, name: String): Unit = {
    db.getTable(name).deleteItem("id", sessionId.toString)
  }

  private def idQuery(id: ObjectId) = Json.obj("id" -> id.toString)
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

  def create(itemId: VersionedId[ObjectId], name: String, orgId: Option[ObjectId] = None): ObjectId = {
    val oid = ObjectId.get
    val baseSession = idQuery(oid) ++ MongoDBObject("itemId" -> itemId.toString)
    val session = orgId match {
      case Some(orgId) => baseSession ++ MongoDBObject("identity" -> MongoDBObject("orgId" -> orgId.toString))
      case _ => baseSession
    }
    db(name).insert(session)
    oid
  }

  def update(sessionId: ObjectId, json: JsValue, name: String): Unit = {
    val dbo = com.mongodb.util.JSON.parse(Json.stringify(json)).asInstanceOf[DBObject]
    db(name).update(idQuery(sessionId), dbo)
  }

  def findSessionForItemId(vid: VersionedId[ObjectId], name: String): ObjectId = {
    db(name).findOne(MongoDBObject("itemId" -> vid.toString()))
      .map(o => o.get("_id").asInstanceOf[ObjectId])
      .getOrElse(throw new RuntimeException(s"Can't find session for item id: $vid"))
  }

  def findSession(id: String, name: String): Option[JsObject] = {
    db(name).findOne(idQuery(id))
      .map(o => Some(Json.parse(o.toString).as[JsObject]))
      .getOrElse(throw new RuntimeException(s"Can't find session with id: $id"))
  }

  def delete(sessionId: ObjectId, name: String): Unit = {
    db(name).remove(MongoDBObject("_id" -> sessionId))
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

  def create(itemId: VersionedId[ObjectId], name: String = v2ItemSessions, orgId: Option[ObjectId] = None): ObjectId = {
    impl.create(itemId, name, orgId)
  }
  def update(sessionId: ObjectId, json: JsValue, name: String = v2ItemSessions): Unit = {
    impl.update(sessionId, json, name)
  }
  def findSessionForItemId(vid: VersionedId[ObjectId], name: String = v2ItemSessions): ObjectId = {
    impl.findSessionForItemId(vid, name)
  }
  def findSession(id: String, name: String = v2ItemSessions): Option[JsObject] = {
    impl.findSession(id, name)
  }
  def delete(sessionId: ObjectId, name: String = v2ItemSessions): Unit = {
    impl.delete(sessionId, name)
  }
}

