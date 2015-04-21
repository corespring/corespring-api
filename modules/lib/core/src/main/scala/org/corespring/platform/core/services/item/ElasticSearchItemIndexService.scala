package org.corespring.platform.core.services.item

import java.net.URL

import org.apache.commons.codec.binary.Base64
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.models.item.index.ItemIndexSearchResult
import play.api.libs.json._
import play.api.libs.ws.WS
import scala.concurrent._
import scalaz._

/**
 * An ItemIndexService based on Elastic Search.
 *
 * TODO: Testing this is *very* difficult, as mocking the native WS in Play < 2.3.x requires starting a Mock HTTP
 * service. When we upgrade ot Play 2.3.x, we should use [play-mockws](https://github.com/leanovate/play-mockws) to
 * test this exhaustively.
 */
class ElasticSearchItemIndexService(elasticSearchUrl: URL) extends ItemIndexService with ComponentMap {

  import ExecutionContext.Implicits.global
  import Base64._

  val authHeader = "Authorization" -> {
    val Array(username, password) = elasticSearchUrl.getUserInfo.split(":")
    s"Basic ${new String(encodeBase64(s"$username:$password".getBytes))}"
  }

  val baseUrl = s"${elasticSearchUrl.getProtocol}://${elasticSearchUrl.getHost}"

  def search(query: ItemIndexQuery): Future[Validation[Error, ItemIndexSearchResult]] = {
    try {
      implicit val QueryWrites = ItemIndexQuery.ElasticSearchWrites
      implicit val ItemIndexSearchResultFormat = ItemIndexSearchResult.Format

      WS.url(baseUrl + "/content/_search").withHeaders(authHeader)
        .post(Json.toJson(query))
        .map(result => Json.fromJson[ItemIndexSearchResult](Json.parse(result.body)) match {
          case JsSuccess(searchResult, _) => Success(searchResult)
          case _ => Failure(new Error("Could not read results"))
        })
    } catch {
      case e: Exception => future { Failure(new Error(e.getMessage)) }
    }
  }

  def distinct(field: String): Future[Validation[Error, Seq[String]]] = {
    try {
      implicit val AggregationWrites = ItemIndexAggregation.Writes
      val agg = ItemIndexAggregation(field = field)
      WS.url(baseUrl + "/content/_search").withHeaders(authHeader)
        .post(Json.toJson(agg))
        .map(result => {
          Success((Json.parse(result.body) \ "aggregations" \ agg.name \ "buckets").as[Seq[JsObject]]
            .map(obj  => (obj \ "key").as[String]))
        })
    } catch {
      case e: Exception => future { Failure(new Error(e.getMessage)) }
    }
  }

  lazy val componentTypes: Future[Validation[Error, Map[String, String]]] =
    distinct("taskInfo.itemTypes").map(result => result.map(itemTypes => itemTypes.map(itemType =>
      componentMap.get(itemType).map(t => t.nonEmpty match {
        case true => Some(t -> itemType)
        case _ => None
      }).flatten
    ).flatten.toMap))
}

object ElasticSearchItemIndexService extends ElasticSearchItemIndexService(AppConfig.elasticSearchUrl)