package org.corespring.itemSearch

import org.corespring.models.ContentCollRef
import play.api.libs.json.Json

import scalaz.Validation
import scalaz.Scalaz._

object QueryStringParser {
  def scopedSearchQuery(query: Option[String], accessibleCollections: Seq[ContentCollRef]): Validation[Error, ItemIndexQuery] = {

    val rawQuery = query.getOrElse("{}")

    for {
      json <- Validation.fromTryCatch(Json.parse(rawQuery))
      iiq <- Json.fromJson[ItemIndexQuery](json).asOpt.toSuccess(new Error(s"Failed to parse query string: $query"))
    } yield {

    }
  }

  private def searchWithQuery(
    q: ItemIndexQuery,
    accessibleCollections: Seq[ContentCollRef]): Future[Validation[Error, ItemIndexSearchResult]] = {
    val accessibleCollectionStrings = accessibleCollections.map(_.collectionId.toString)
    val collections = q.collections.filter(id => accessibleCollectionStrings.contains(id))
    val scopedQuery = collections.isEmpty match {
      case true => q.copy(collections = accessibleCollectionStrings)
      case _ => q.copy(collections = collections)
    }

    logger.trace(s"function=searchWithQuery, scopedQuery=$scopedQuery")
    itemIndexService.search(scopedQuery)
  }

}
