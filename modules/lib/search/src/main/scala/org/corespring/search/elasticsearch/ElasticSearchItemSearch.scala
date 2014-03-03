package org.corespring.search.elasticsearch

import org.corespring.search.indexing.ItemSearch
import play.api.libs.json._
import com.sksamuel.elastic4s.ElasticDsl._
import scala.concurrent.ExecutionContext
import ElasticSearchClient.client
import org.elasticsearch.action.search.SearchResponse
import org.apache.commons.io.IOUtils
import org.bson.types.ObjectId

object ElasticSearchItemSearch extends ItemSearch {
  import ExecutionContext.Implicits.global

  def baseSearch = search in "content"

  def find(searchText: Option[String],
           collectionIds: Seq[ObjectId],
           fields: Seq[String],
           skip: Option[Int],
           limit: Option[Int],
           sort: Option[String]) = {
    client.execute {
      baseSearch.withQuery(searchText).withCollectionIds(collectionIds).skip(skip).limit(limit).sort(sort)
    }.map(searchResponse => fromElasticResults(searchResponse, fields) match {
      case Left(jsObject: JsArray) => jsObject
      case _ => Json.arr()
    })
  }

  private def fromElasticResults(searchResponse: SearchResponse, fields: Seq[String]): Either[JsValue, String] = {
    val filterTransform = {jsValue: JsValue => {
      fields.foldLeft(Json.obj())((json, field) => json + (field -> (jsValue \ field)))
    }}

    Left(JsArray(searchResponse
      .getHits
      .hits
      .map(hit => Json.parse(IOUtils.toString(hit.source, "UTF-8"))).transform(filterTransform)))
  }

  /**
   * Wrapper for SearchDefinition
   */
  implicit class SearchHelper(searchDefinition: SearchDefinition) {

    def withQuery(queryString: Option[String]) = queryString match {
      case Some(string) => searchDefinition query string
      case _ => searchDefinition
    }

    def withCollectionIds(collectionIds: Seq[ObjectId]) = collectionIds.nonEmpty match {
      case true => searchDefinition filter {
        termsFilter("collectionId", collectionIds.map(_.toString):_*)
      }
      case _ => searchDefinition
    }

    def skip(skip: Option[Int]) = skip match {
      case Some(skipVal) => searchDefinition from skipVal
      case _ => searchDefinition
    }

    def limit(limit: Option[Int]) = limit match {
      case Some(limit) => searchDefinition size limit
      case _ => searchDefinition
    }

    def sort(sort: Option[String]) = sort match {
      case Some(sortField) => searchDefinition sort ( by field sortField )
      case _ => searchDefinition
    }

  }

}
