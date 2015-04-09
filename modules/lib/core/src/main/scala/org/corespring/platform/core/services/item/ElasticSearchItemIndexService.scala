package org.corespring.platform.core.services.item

import java.net.URL

import org.apache.commons.codec.binary.Base64
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.models.item.index.ItemIndexSearchResult
import play.api.libs.json._
import play.api.libs.ws.WS
import scala.concurrent._
import scalaz._

class ElasticSearchItemIndexService(elasticSearchUrl: URL) extends ItemIndexService {

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
      val opts = Json.toJson(query)
      println(Json.prettyPrint(opts))
      WS.url(baseUrl + "/content/_search").withHeaders(authHeader)
        .post(opts)
        .map(result => Json.fromJson[ItemIndexSearchResult](Json.parse(result.body)) match {
          case JsSuccess(searchResult, _) => Success(searchResult)
          case _ => Failure(new Error("Could not read results"))
        })
    } catch {
      case e: Exception => future { Failure(new Error(e.getMessage)) }
    }
  }

}

object ElasticSearchItemIndexService extends ElasticSearchItemIndexService(AppConfig.elasticSearchUrl)