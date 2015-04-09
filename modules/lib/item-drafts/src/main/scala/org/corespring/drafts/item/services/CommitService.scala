package org.corespring.drafts.item.services

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{ DBObject, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.item.models.ItemCommit
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime

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
    val query = MongoDBObject("srcId._id" -> id, "srcId.version" -> version)
    collection.find(query).toSeq.map(toCommit)
  }

  def save(c: ItemCommit): WriteResult = {
    collection.save(toDbo(c))
  }

  def findCommitsSince(id: VersionedId[ObjectId], date: DateTime): Seq[ItemCommit] = {

    val query = MongoDBObject(
      "date" -> MongoDBObject(
        "$gt" -> date),
      "srcId._id" -> id.id)

    val stream = collection.find(query)
    println(s"stream: $stream")
    val out = stream.toSeq.map(toCommit)
    println(s"out: $out")
    out
  }
}
