package org.corespring.platform.core.services

<<<<<<< HEAD
import com.mongodb.DBObject
import com.mongodb.casbah.commons.{ MongoDBList, MongoDBObject }
import org.bson.types.ObjectId
import org.corespring.common.log.PackageLogging
import org.corespring.models.{ Standard, Subject }
import play.api.libs.json.{ JsObject, JsSuccess, JsValue, Json }
=======
import org.bson.types.ObjectId
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.{ Standard }
>>>>>>> develop

object StandardQueryService extends QueryService[Standard] with StandardQueryBuilder with PackageLogging {

  override def findOne(id: String): Option[Standard] = if (ObjectId.isValid(id)) {
    logger.trace(s"findOne: $id")
    Standard.findOneById(new ObjectId(id))
  } else None

  override def list(): Seq[Standard] = {
    logger.trace(s"list")
    Standard.findAll().toSeq
  }

  override def query(raw: String): Seq[Standard] = {
    getQuery(raw).map(query => {
      logger.trace(s"mongo query: ${query}")
      Standard.find(query).toSeq
    }).getOrElse(Seq.empty[Standard])
  }

  def getQuery(raw: String) = {
    getStandardByDotNotationQuery(raw).orElse(getStandardBySearchQuery(raw))
  }

<<<<<<< HEAD
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
    json.map(filters => for ((k, v) <- filters.fields) { query.put(k, v.as[String]) })
    query
  }

  private def toRegex(searchTerm: String) = MongoDBObject("$regex" -> searchTerm, "$options" -> "i")

=======
>>>>>>> develop
}
