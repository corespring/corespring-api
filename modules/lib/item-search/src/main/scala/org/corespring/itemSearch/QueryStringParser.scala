package org.corespring.itemSearch

import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.Json

import scalaz.Validation
import scalaz.Scalaz._
import ItemIndexQuery.ApiReads

object QueryStringParser {

  private val logger = Logger(QueryStringParser.getClass)

  def scopedSearchQuery(query: Option[String], accessibleCollectionIds: Seq[ObjectId]): Validation[Error, ItemIndexQuery] = {
    val rawQuery = query.getOrElse("{}")
    logger.trace(s"function=scopedSearchQuery, rawQuery=$rawQuery")

    implicit val r = ApiReads

    for {
      json <- Validation.fromTryCatch(Json.parse(rawQuery)).leftMap(e => new Error(s"Error parsing query: $e"))
      iiq <- Json.fromJson[ItemIndexQuery](json).asOpt.toSuccess(new Error(s"Failed to parse query string: $query"))
    } yield {
      iiq.scopeToCollections(accessibleCollectionIds.map(_.toString): _*)
    }
  }
}
