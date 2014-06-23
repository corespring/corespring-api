package org.corespring.platform.core.services

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.Subject

object SubjectQueryService extends QueryService[Subject] with PackageLogging {

  override def findOne(id: String): Option[Subject] = if (ObjectId.isValid(id)) {
    logger.trace(s"findOne: $id")
    Subject.findOneById(new ObjectId(id))
  } else None

  override def list(): Seq[Subject] = {
    logger.trace(s"list")
    Subject.findAll().toSeq
  }

  override def query(term: String): Seq[Subject] = {

    val query = MongoDBObject(
      "subject" ->
        MongoDBObject(
          "$regex" -> s"$term"))
    logger.trace(s"mongo query: ${query}")
    Subject.find(query).toSeq
  }
}
