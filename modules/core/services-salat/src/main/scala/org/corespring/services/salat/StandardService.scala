package org.corespring.services.salat

import com.mongodb.DBObject
import com.mongodb.casbah.commons.{ MongoDBList, MongoDBObject }
import grizzled.slf4j.Logger
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import org.corespring.models.{ Standard, Domain }
import play.api.libs.json.{ JsObject, JsValue, Json }

import scala.concurrent._
import scala.concurrent.duration.Duration

class StandardService(val dao: SalatDAO[Standard, ObjectId],
  val context: Context) extends org.corespring.services.StandardService with HasDao[Standard, ObjectId] {

  private val logger = Logger(classOf[StandardService])

  override def findOneById(id: ObjectId): Option[Standard] = dao.findOneById(id)

  import ExecutionContext.Implicits.global

  val timeout = Duration(20, duration.SECONDS)

  override lazy val domains: Map[String, Seq[Domain]] = {

    def combineFutures(results: Seq[Future[(String, Seq[Domain])]]) =
      Await.result(Future.sequence(results), timeout).toMap

    /**
     * Transforms an Iterator of standards into Domains
     * @param getDomain function describing the property of each standard to be used as the name for the Domain
     */
    def mapDomains(standards: Iterator[Standard], getDomain: (Standard => Option[String])) =
      standards.foldLeft(Map.empty[String, Seq[String]]) {
        case (map, standard) => getDomain(standard) match {
          case Some(domain) => map.get(domain) match {
            case Some(standards) =>
              map + (domain -> standard.dotNotation.map(standard => standards :+ standard).getOrElse(standards))
            case _ => map + (domain -> standard.dotNotation.map(Seq(_)).getOrElse(Seq.empty))
          }
          case _ => map
        }
      }.map { case (name, standards) => new Domain(name, standards) }.toSeq

    import Standard.Subjects
    import Standard.Keys._

    combineFutures(Seq(
      future {
        Subjects.ELA -> mapDomains(dao.find(MongoDBObject(
          Subject -> MongoDBObject("$in" -> Seq(Subjects.ELA, Subjects.ELALiteracy)))), {
          _.subCategory
        })
      },
      future {
        Subjects.Math -> mapDomains(dao.find(MongoDBObject(
          Subject -> Subjects.Math)), {
          _.category
        })
      }))
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

}
