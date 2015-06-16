package org.corespring.v2.auth.wired

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}
import org.bson.types.ObjectId
import org.corespring.v2.auth.services.SessionService
import play.api.Play
import play.api.libs.json.{JsObject, JsValue, Json}


/**
 * Writes/Reads session to db as pair of (sessionId,json)
 */
class DynamoSessionService(tableName:String) extends SessionService {

  val itemIdKey = "itemId"
  val jsonKey = "json"
  val sessionIdKey = "id"

  lazy val dynamoDB = new DynamoDB(new AmazonDynamoDBClient(new ProfileCredentialsProvider{
    val configuration = Play.current.configuration

    def getAWSAccessKeyId: String = configuration.getString("amazon.s3.key").getOrElse("")
    def getAWSSecretKey: String = configuration.getString("amazon.s3.secret").getOrElse("")
  }))

  lazy val table = dynamoDB.getTable(tableName)

  override def create(data: JsValue): Option[ObjectId] = {
    val sessionId = (data \ sessionIdKey).asOpt[String].getOrElse((new ObjectId).toString)
    val itemId = (data \ itemIdKey).asOpt[String].getOrElse("")
    val json = data.toString;
    val item = new Item().withPrimaryKey(sessionIdKey, sessionId)
    if(!itemId.isEmpty) {
      item.withString(itemIdKey, itemId)
    }
    if(!json.isEmpty) {
      item.withJSON(jsonKey, data.toString)
    }
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
    val item = new Item()
      .withPrimaryKey(sessionIdKey, sessionId)
      .withString(itemIdKey, itemId)
      .withJSON(jsonKey, data.toString)
    table.putItem(item)
    Some(data)
  }

}
