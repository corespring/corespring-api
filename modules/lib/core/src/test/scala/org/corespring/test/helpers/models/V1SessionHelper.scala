package org.corespring.test.helpers.models

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play
import se.radley.plugin.salat.SalatPlugin

object V1SessionHelper {

  val v1ItemSessions = "itemsessions"

  lazy val db = {
    Play.current.plugin[SalatPlugin].map {
      p =>
        p.db()
    }.getOrElse(throw new RuntimeException("Error loading salat plugin"))
  }

  def create(itemId: VersionedId[ObjectId]): ObjectId = {
    val oid = ObjectId.get
    db("itemsessions").insert(MongoDBObject(
      "_id" -> oid,
      "itemId" -> itemIdToMongo(itemId)))
    oid
  }

  def findSessionForItemId(vid: VersionedId[ObjectId], name: String = v1ItemSessions): ObjectId = {
    db(name).findOne(MongoDBObject("itemId" -> itemIdToMongo(vid)))
      .map(o => o.get("_id").asInstanceOf[ObjectId])
      .getOrElse(throw new RuntimeException(s"Can't find sesssion for item id: $vid"))
  }

  def delete(sessionId: ObjectId, name: String = v1ItemSessions): Unit = {
    db(name).remove(MongoDBObject("_id" -> sessionId))
  }

  private def itemIdToMongo(itemId: VersionedId[ObjectId]) = {
    MongoDBObject("_id" -> itemId.id, "version" -> itemId.version)
  }

}
