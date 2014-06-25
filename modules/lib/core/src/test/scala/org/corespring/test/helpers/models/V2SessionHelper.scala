package org.corespring.test.helpers.models

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play
import se.radley.plugin.salat.SalatPlugin

object V2SessionHelper {

  lazy val collection = {
    Play.current.plugin[SalatPlugin].map {
      p =>
        p.db()("v2.itemSessions")

    }.getOrElse(throw new RuntimeException("Error loading salat plugin"))
  }

  def create(itemId: VersionedId[ObjectId]): ObjectId = {
    val oid = ObjectId.get
    collection.insert(MongoDBObject(
      "_id" -> oid,
      "itemId" -> itemId.toString))
    oid
  }

  def findSessionForItemId(vid: VersionedId[ObjectId]): ObjectId = {
    collection.findOne(MongoDBObject("itemId" -> vid.toString()))
      .map(o => o.get("_id").asInstanceOf[ObjectId])
      .getOrElse(throw new RuntimeException(s"Can't find sesssion for item id: $vid"))
  }

  def delete(sessionId: ObjectId): Unit = {
    collection.remove(MongoDBObject("_id" -> sessionId))
  }

}
