package org.corespring.platform.core.services

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import play.api.libs.json.{ JsValue, Json, JsObject }

private[services] trait StandardQueryBuilder {

  lazy val searchTermKeys = Seq("standard", "subject", "category", "subCategory", "dotNotation")

  def getStandardByDotNotationQuery(raw: String): Option[DBObject] = for {
    json <- Json.parse(raw).asOpt[JsValue]
    dotNotation <- (json \ "dotNotation").asOpt[String]
  } yield MongoDBObject("dotNotation" -> dotNotation)

  def getStandardBySearchQuery(raw: String): Option[DBObject] = for {
    json <- Json.parse(raw).asOpt[JsValue]
  } yield {
    Seq((json \ "filters").asOpt[JsObject].map(filterDbo),
      (json \ "searchTerm").asOpt[String].map(searchTermDbo))
      .flatten
      .fold(MongoDBObject.empty) { (acc, dbo) =>
        new MongoDBObject(acc) ++ dbo
      }
  }

  private def toRegex(searchTerm: String) = MongoDBObject("$regex" -> searchTerm, "$options" -> "i")

  def filterDbo(filters: JsObject): DBObject = {
    val query = MongoDBObject.empty
    for ((k, v) <- filters.fields) { query.put(k, v.as[String]) }
    query
  }

  def searchTermDbo(searchTerm: String): DBObject = {
    import com.mongodb.casbah.Imports._
    val r = toRegex(searchTerm)
    $or(searchTermKeys.map(_ -> r): _*)
  }

}