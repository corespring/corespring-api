package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import grizzled.slf4j.Logger
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import org.corespring.models.{ Domain, StandardDomains, Standard }
import org.corespring.services.StandardQuery
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import play.api.libs.json.{ JsObject, JsValue, Json }

import scala.concurrent.{ ExecutionContext, Future }

class StandardService(val dao: SalatDAO[Standard, ObjectId],
  val servicesExecutionContext: SalatServicesExecutionContext,
  val context: Context) extends org.corespring.services.StandardService with HasDao[Standard, ObjectId] {

  private implicit val ec: ExecutionContext = servicesExecutionContext.ctx

  private val logger = Logger(classOf[StandardService])

  override def findOneById(id: ObjectId): Option[Standard] = dao.findOneById(id)

  private def getDomains(getDomain: Standard => Option[String], subjects: String*): Future[Seq[Domain]] = {
    val query = Standard.Keys.Subject $in subjects
    logger.trace(s"function=getDomains, query=$query")
    val standards = Future { dao.find(query).toSeq }
    standards.map { s =>
      logger.trace(s"function=getDomains, query=$query, standards=$s, size=${s.size}")
      Domain.fromStandards(s, _.subCategory)
    }
  }

  //Core Refactor: From Standard.Domains
  override lazy val domains: Future[StandardDomains] = {
    import Standard.{ Subjects }
    Future.sequence(Seq(
      getDomains(_.subCategory, Subjects.ELA, Subjects.ELALiteracy),
      getDomains(_.category, Subjects.Math))).map { results =>
      StandardDomains(results(0), results(1))
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

  override def queryDotNotation(dotNotation: String, l: Int, sk: Int): Stream[Standard] = {
    val query = MongoDBObject("dotNotation" -> dotNotation)
    dao.find(query).skip(sk).limit(l).toStream
  }

  private def toDbo(q: StandardQuery): DBObject = {

    val l = List(
      q.category.map("category" -> _),
      q.subject.map("subject" -> _),
      q.standard.map("standard" -> _),
      q.subCategory.map("subCategory" -> _)).flatten

    val query = MongoDBObject(l)

    def invert(key: String, k: Option[String]): Option[DBObject] = {
      if (k.isEmpty) Some(MongoDBObject(key -> toRegex(q.term))) else None
    }

    val queryList = List(
      Some(MongoDBObject("dotNotation" -> q.term)),
      invert("category", q.category),
      invert("subject", q.subject),
      invert("standard", q.standard),
      invert("subCategory", q.subCategory)).flatten
    import com.mongodb.casbah.Imports._
    val or = $or(queryList)
    query ++ or
  }

  override def query(q: StandardQuery, l: Int = 50, sk: Int = 0): Stream[Standard] = {
    dao.find(toDbo(q)).limit(l).skip(sk).toStream
  }

  @deprecated("use 'query(Query) instead", "core-refactor")
  override def query(term: String): Stream[Standard] = {

    lazy val standardByDotNotationQuery: Option[DBObject] = for {
      json <- Json.parse(term).asOpt[JsValue]
      dotNotation <- (json \ "dotNotation").asOpt[String]
    } yield MongoDBObject("dotNotation" -> dotNotation)

    lazy val standardBySearchQuery: Option[DBObject] = for {
      json <- Json.parse(term).asOpt[JsValue]
      searchTerm <- (json \ "searchTerm").asOpt[String]
    } yield addFilters(MongoDBObject("$or" -> MongoDBList(
      MongoDBObject("standard" -> toRegex(searchTerm)),
      MongoDBObject("subject" -> toRegex(searchTerm)),
      MongoDBObject("category" -> toRegex(searchTerm)),
      MongoDBObject("subCategory" -> toRegex(searchTerm)),
      MongoDBObject("dotNotation" -> toRegex(searchTerm)))), (json \ "filters").asOpt[JsObject])

    lazy val queryObject = {
      standardByDotNotationQuery.orElse(standardBySearchQuery)
    }

    def addFilters(query: DBObject, json: Option[JsObject]): DBObject = {
      json.map(filters => for ((k, v) <- filters.fields) {
        query.put(k, v.as[String])
      })
      query
    }

    queryObject.map { q =>
      dao.find(q).toStream
    }.getOrElse(Stream.empty[Standard])
  }

  private def toRegex(searchTerm: String) = MongoDBObject("$regex" -> searchTerm, "$options" -> "i")

  override def count(query: DBObject): Long = dao.count(query)

  override def find(dbo: DBObject): Stream[Standard] = dao.find(dbo).toStream

  override def insert(standard: Standard): Option[ObjectId] = dao.insert(standard, WriteConcern.Safe)

  override def delete(id: ObjectId): Boolean = dao.removeById(id).getN == 1

}
