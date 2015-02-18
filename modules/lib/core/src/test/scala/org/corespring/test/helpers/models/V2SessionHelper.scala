package org.corespring.test.helpers.models

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play
import play.api.libs.json.{ Json, JsValue }
import se.radley.plugin.salat.SalatPlugin
import com.mongodb.casbah.Imports._

object V2SessionHelper {

  val v2ItemSessions = "v2.itemSessions"
  val v2ItemSessionsPreview = "v2.itemSessions_preview"

  import scala.language.implicitConversions

  private implicit def strToObjectId(id: String) = new ObjectId(id)

  lazy val db = {
    Play.current.plugin[SalatPlugin].map {
      p =>
        p.db()
    }.getOrElse(throw new RuntimeException("Error loading salat plugin"))
  }

  def create(itemId: VersionedId[ObjectId], name: String = v2ItemSessions): ObjectId = {
    val oid = ObjectId.get
    db(name).insert(idQuery(oid) ++ MongoDBObject("itemId" -> itemId.toString))
    oid
  }

  def update(sessionId: ObjectId, json: JsValue, name: String = v2ItemSessions): Unit = {
    val dbo = com.mongodb.util.JSON.parse(Json.stringify(json)).asInstanceOf[DBObject]
    db(name).update(idQuery(sessionId), dbo)
  }

  def findSessionForItemId(vid: VersionedId[ObjectId], name: String = v2ItemSessions): ObjectId = {
    db(name).findOne(MongoDBObject("itemId" -> vid.toString()))
      .map(o => o.get("_id").asInstanceOf[ObjectId])
      .getOrElse(throw new RuntimeException(s"Can't find sesssion for item id: $vid"))
  }

  def findSession(id: String, name: String = v2ItemSessions): DBObject = {
    db(name).findOne(idQuery(id)).getOrElse {
      throw new RuntimeException(s"Can't find session with id: $id")
    }
  }

  def delete(sessionId: ObjectId, name: String = v2ItemSessions): Unit = {
    db(name).remove(MongoDBObject("_id" -> sessionId))
  }

  private def idQuery(id: ObjectId) = MongoDBObject("_id" -> id)
}
