package org.corespring.services.salat

import com.mongodb.DBObject
import com.mongodb.casbah.WriteConcern
import com.mongodb.casbah.commons.{ MongoDBList, MongoDBObject }
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.Subject
import org.corespring.services
import play.api.libs.json.{ JsValue, Json }

class SubjectService(dao: SalatDAO[Subject, ObjectId], context: Context) extends services.SubjectService {

  private val logger = Logger(classOf[SubjectService])

  override def findOneById(id: ObjectId): Option[Subject] = dao.findOneById(id)

  override def findOne(id: String): Option[Subject] = if (ObjectId.isValid(id)) {
    logger.trace(s"findOne: $id")
    dao.findOneById(new ObjectId(id))
  } else None

  override def list(): Stream[Subject] = {
    logger.trace(s"list")
    dao.find(MongoDBObject.empty).toStream
  }

  override def query(raw: String): Stream[Subject] = {
    getQuery(raw).map(query => {
      logger.trace(s"mongo query: ${query}")
      dao.find(query).toStream
    }).getOrElse(Stream.empty[Subject])
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

  private def toRegex(searchTerm: String) = MongoDBObject("$regex" -> searchTerm, "$options" -> "i")

  override def count(query: DBObject): Long = dao.count(query)

  override def find(dbo: DBObject): Stream[Subject] = dao.find(dbo).toStream

  override def insert(s: Subject): Option[ObjectId] = dao.insert(s)

  override def delete(id: ObjectId): Boolean = dao.removeById(id).getN == 1
}