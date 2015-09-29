package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import grizzled.slf4j.Logger
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import org.corespring.models.{ Domain, StandardDomains, Standard }
import play.api.libs.json.{ JsObject, JsValue, Json }

import scala.concurrent.Future

class StandardService(val dao: SalatDAO[Standard, ObjectId],
  val context: Context) extends org.corespring.services.StandardService with HasDao[Standard, ObjectId] {

  private val logger = Logger(classOf[StandardService])

  override def findOneById(id: ObjectId): Option[Standard] = dao.findOneById(id)

  private def getDomains(subjects: String*): Future[Seq[Domain]] = Future {
    import Standard.{ Keys }
    val query = Keys.Subject $in subjects
    val standards = dao.find(query)
    Domain.fromStandards(standards.toSeq, _.subCategory)
  }

  //Core Refactor: From Standard.Domains
  override lazy val domains: Future[StandardDomains] = {
    import Standard.{ Subjects }
    for {
      ela <- getDomains(Subjects.ELA, Subjects.ELALiteracy)
      math <- getDomains(Subjects.Math)
    } yield {
      StandardDomains(ela, math)
    }
  }

  override def findOneByDotNotation(dotNotation: String): Option[Standard] = dao.findOne(MongoDBObject("dotNotation" -> dotNotation))

  override def findOne(id: String): Option[Standard] = if (ObjectId.isValid(id)) {
    logger.trace(s"findOne: $id")
    dao.findOneById(new ObjectId(id))
  } else None

  override def list(): Stream[Standard] = {
    logger.trace(s"list")
    dao.find(MongoDBObject.empty).toStream
  }

  override def query(raw: String): Stream[Standard] = {
    getQuery(raw).map(query => {
      logger.trace(s"mongo query: ${query}")
      dao.find(query).toStream
    }).getOrElse(Stream.empty[Standard])
  }

  def getQuery(raw: String) = {
    getStandardByDotNotationQuery(raw).orElse(getStandardBySearchQuery(raw))
  }

  private def getStandardByDotNotationQuery(raw: String): Option[DBObject] = for {
    json <- Json.parse(raw).asOpt[JsValue]
    dotNotation <- (json \ "dotNotation").asOpt[String]
  } yield MongoDBObject("dotNotation" -> dotNotation)

  private def getStandardBySearchQuery(raw: String): Option[DBObject] = for {
    json <- Json.parse(raw).asOpt[JsValue]
    searchTerm <- (json \ "searchTerm").asOpt[String]
  } yield addFilters(MongoDBObject("$or" -> MongoDBList(
    MongoDBObject("standard" -> toRegex(searchTerm)),
    MongoDBObject("subject" -> toRegex(searchTerm)),
    MongoDBObject("category" -> toRegex(searchTerm)),
    MongoDBObject("subCategory" -> toRegex(searchTerm)),
    MongoDBObject("dotNotation" -> toRegex(searchTerm)))), (json \ "filters").asOpt[JsObject])

  private def addFilters(query: DBObject, json: Option[JsObject]): DBObject = {
    json.map(filters => for ((k, v) <- filters.fields) {
      query.put(k, v.as[String])
    })
    query
  }

  private def toRegex(searchTerm: String) = MongoDBObject("$regex" -> searchTerm, "$options" -> "i")

  override def count(query: DBObject): Long = dao.count(query)

  override def find(dbo: DBObject): Stream[Standard] = dao.find(dbo).toStream

  override def insert(standard: Standard): Option[ObjectId] = dao.insert(standard)

  override def delete(id: ObjectId): Boolean = dao.removeById(id).getN == 1

}
