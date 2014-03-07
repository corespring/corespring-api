package org.corespring.search.elasticsearch.indexing

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * A helper object to generate rivers and indexes for elasticsearch using the WebService api
 */
object IndexerWs extends Indexer with RiverConfigUtil {

  /**
   * TODO: This should only drop collectionsToSlurp, but this seemed to be nontrivial
   */
  override protected def dropAll() = elasticSearchWs("_all").delete()

  override protected def importMapping() = {
    Await.result(elasticSearchWs("content").put(ContentMapping.readJson), Duration.Inf)
  }

  override protected def createRivers() = rivers.map(createRiver(_))

  val rivers = Seq(
    River(name = "content", typ = "content", collection = "content"),
    River(name = "standards", typ = "standard", collection = "ccstandards"))

  def createRiver(river: River) = {
    val source = createRiverConfigJson(river).toString
    elasticSearchWs(s"_river/${river.name}/_meta").put(source)
  }


}
