package org.corespring.test.helpers.models

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play
import se.radley.plugin.salat.SalatPlugin

object V2SessionHelper {

  val v2ItemSessions = "v2.itemSessions"
  val v2ItemSessionsPreview = "v2.itemSessions_preview"

  lazy val db = {
    Play.current.plugin[SalatPlugin].map {
      p =>
        p.db()
    }.getOrElse(throw new RuntimeException("Error loading salat plugin"))
  }

  def create(itemId: VersionedId[ObjectId], name: String = v2ItemSessions): ObjectId = {
    val oid = ObjectId.get
    db(name).insert(MongoDBObject(
      "_id" -> oid,
      "itemId" -> itemId.toString))
    oid
  }

  def findSessionForItemId(vid: VersionedId[ObjectId], name: String = v2ItemSessions): ObjectId = {
    db(name).findOne(MongoDBObject("itemId" -> vid.toString()))
      .map(o => o.get("_id").asInstanceOf[ObjectId])
      .getOrElse(throw new RuntimeException(s"Can't find sesssion for item id: $vid"))
  }

  def delete(sessionId: ObjectId, name: String = v2ItemSessions): Unit = {
    db(name).remove(MongoDBObject("_id" -> sessionId))
  }

}
