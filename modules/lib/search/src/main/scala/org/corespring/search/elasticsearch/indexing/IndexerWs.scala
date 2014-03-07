package org.corespring.search.elasticsearch.indexing

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.corespring.platform.core.models.JsonUtil

/**
 * A helper object to generate rivers and indexes for elasticsearch using the WebService api
 */
object IndexerWs extends Indexer with RiverService with JsonUtil {

  /**
   * TODO: This should only drop collectionsToSlurp, but this seemed to be nontrivial
   */
  override protected def dropAll() = elasticSearchWs("_all").delete()

  override protected def importMapping() = {
    Await.result(elasticSearchWs("content").put(ContentMapping.readJson), Duration.Inf)
  }

  val rivers = Seq(
    River(name = "content", typ = "content", collection = "content"),
    River(name = "standards", typ = "standard", collection = "ccstandards"))

  override protected def createRivers() = rivers.map(createRiver(_))

}
