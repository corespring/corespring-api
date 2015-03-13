package org.corespring.drafts.item.services

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{ DBObject, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.item.models.ItemCommit

trait CommitService {

  def collection: MongoCollection

  import com.novus.salat.grater
  import org.corespring.platform.core.models.mongoContext.context

  private def toCommit(dbo: DBObject): ItemCommit = {
    grater[ItemCommit].asObject(new MongoDBObject(dbo))
  }

  private def toDbo(c: ItemCommit): DBObject = {
    grater[ItemCommit].asDBObject(c)
  }

  def findByIdAndVersion(id: ObjectId, version: Long): Seq[ItemCommit] = {
    val query = MongoDBObject("srcVid._id" -> id, "srcVid.version" -> version)
    collection.find(query).toSeq.map { dbo => toCommit(dbo) }
  }

  def save(c: ItemCommit): WriteResult = {
    collection.save(toDbo(c))
  }
}
