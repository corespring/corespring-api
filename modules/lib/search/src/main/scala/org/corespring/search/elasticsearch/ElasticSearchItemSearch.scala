package org.corespring.search.elasticsearch

import org.corespring.search.indexing.ItemSearch
import play.api.libs.json.Json
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import scala.concurrent.ExecutionContext
import org.elasticsearch.common.settings.ImmutableSettings

object ElasticSearchItemSearch extends ItemSearch {
  import ExecutionContext.Implicits.global

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", "corespring_elasticsearch").build()
  val client = ElasticClient.remote(settings, ("localhost", 9300))

  def find(query: String) = {
    client.execute {
      search in "content" query query
    }.map(searchResponse => {
      Json.obj("response" -> searchResponse.toString)
    })
  }

}
