package org.corespring.itemSearch

import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.Json

import scalaz.Validation
import scalaz.Scalaz._
import ItemIndexQuery.ApiReads

object QueryStringParser {

  private val logger = Logger(QueryStringParser.getClass)

  def invalidJson(e: String) = new Error(s"Error parsing query: $e")

  def notValidQuery(s: String) = new Error(s"Failed to parse query string: $s")

  private def parse(s: String) = Validation.fromTryCatch(Json.parse(s)).leftMap(_ => invalidJson(s))

  def scopedSearchQuery(query: Option[String], accessibleCollectionIds: Seq[ObjectId]): Validation[Error, ItemIndexQuery] = {
    val rawQuery = query.getOrElse("{}")
    logger.trace(s"function=scopedSearchQuery, rawQuery=$rawQuery")

    implicit val r = ApiReads

    for {
      json <- parse(rawQuery)
      iiq <- Json.fromJson[ItemIndexQuery](json).asOpt.toSuccess(notValidQuery(rawQuery))
    } yield {
      iiq.scopeToCollections(accessibleCollectionIds.map(_.toString): _*)
    }
  }
}
