package org.corespring.passage.search

import grizzled.slf4j.Logger
import org.corespring.itemSearch._
import play.api.libs.json.{JsSuccess, Json}

import scala.concurrent._
import scalaz.{Failure, Success, Validation}

class ElasticSearchPassageIndexService(config: ElasticSearchConfig,
  executionContext: ElasticSearchExecutionContext) extends PassageIndexService with AuthenticatedUrl {

  implicit def ec: ExecutionContext = executionContext.context

  private val logger = Logger(classOf[ElasticSearchItemIndexService])

  implicit val url = config.url

  override def search(query: PassageIndexQuery): Future[Validation[Error, PassageIndexSearchResult]] = try {

    implicit val QueryWrites = PassageIndexQuery.ElasticSearchWrites
    implicit val PassageIndexSearchResultFormat = PassageIndexSearchResult.Format

    val queryJson = Json.toJson(query)
    logger.trace(s"function=search, query=$queryJson")

    authed("/passages/_search")(url, ec)
      .post(queryJson)
      .map(result => Json.fromJson[PassageIndexSearchResult](Json.parse(result.body)) match {
      case JsSuccess(searchResult, _) => Success(searchResult)
      case _ => Failure(new Error("Could not read results"))
    })
  } catch {
    case e: Exception => future { Failure(new Error(e.getMessage)) }
  }

}
