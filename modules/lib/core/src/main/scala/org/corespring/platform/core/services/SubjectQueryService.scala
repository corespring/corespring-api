package org.corespring.platform.core.services

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.{ Standard, Subject }
import play.api.libs.json.Json

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

object StandardQueryService extends QueryService[Standard] with PackageLogging {

  override def findOne(id: String): Option[Standard] = if (ObjectId.isValid(id)) {
    logger.trace(s"findOne: $id")
    Standard.findOneById(new ObjectId(id))
  } else None

  override def list(): Seq[Standard] = {
    logger.trace(s"list")
    Standard.findAll().toSeq
  }

  private def searchTerm(raw: String): String = {
    try {
      /**
       * Note: this is a temporary workaround to extract the search term from a json string.
       * see: https://thesib.atlassian.net/browse/CA-1558
       */
      val json = Json.parse(raw)
      (json \ "searchTerm").as[String]
    } catch {
      case _: Throwable => raw
    }
  }

  override def query(raw: String): Seq[Standard] = {
    val term = searchTerm(raw)
    /**
     * Note: This query will need to be expanded to search for the term in the following fields:
     * [subject,dotNotation,category,subCategory,standard]
     * see: https://thesib.atlassian.net/browse/CA-1558
     */
    val query = MongoDBObject(
      "standard" ->
        MongoDBObject(
          "$regex" -> s"$term"))
    logger.trace(s"mongo query: ${query}")
    Standard.find(query).toSeq
  }
}
