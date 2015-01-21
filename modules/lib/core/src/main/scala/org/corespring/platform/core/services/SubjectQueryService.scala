package org.corespring.platform.core.services

import com.mongodb.DBObject
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import org.bson.types.ObjectId
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.{ Standard, Subject }
import play.api.libs.json.{ JsObject, JsSuccess, JsValue, Json }

object SubjectQueryService extends QueryService[Subject] with PackageLogging {

  override def findOne(id: String): Option[Subject] = if (ObjectId.isValid(id)) {
    logger.trace(s"findOne: $id")
    Subject.findOneById(new ObjectId(id))
  } else None

  override def list(): Seq[Subject] = {
    logger.trace(s"list")
    Subject.findAll().toSeq
  }

  override def query(raw: String): Seq[Subject] = {
    getQuery(raw).map(query => {
      logger.trace(s"mongo query: ${query}")
      Subject.find(query).toSeq
    }).getOrElse(Seq.empty[Subject])
  }

  def getQuery(raw: String) = {
    getSimpleSubjectQuery(raw).orElse(getSubjectByCategoryAndSubjectQuery(raw))
  }

  private def getSimpleSubjectQuery(raw: String): Option[DBObject] = for {
    json <- Json.parse(raw).asOpt[JsValue]
    searchTerm <- (json \ "searchTerm").asOpt[String]
  } yield MongoDBObject("$or" -> MongoDBList(
      MongoDBObject("subject" -> toRegex(searchTerm)),
      MongoDBObject("category" -> toRegex(searchTerm))))

  private def getSubjectByCategoryAndSubjectQuery(raw: String): Option[DBObject] = for {
    json <- Json.parse(raw).asOpt[JsValue]
    filters <- (json \ "filters").asOpt[JsValue]
    category <- (filters \ "category").asOpt[String]
  } yield addOptional(MongoDBObject("category" -> category), (filters \ "subject").asOpt[String])

  def addOptional(query: DBObject, json: Option[String]): DBObject = {
    json.map(s => query.put("subject", s))
    query
  }

  private def toRegex( searchTerm: String ) =  MongoDBObject("$regex" -> searchTerm, "$options" -> "i")
}

