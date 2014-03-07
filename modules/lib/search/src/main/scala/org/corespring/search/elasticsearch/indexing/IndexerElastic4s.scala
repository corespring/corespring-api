package org.corespring.search.elasticsearch.indexing

import com.sksamuel.elastic4s.ElasticDsl._
import org.corespring.search.elasticsearch.ElasticSearchClient
import com.sksamuel.elastic4s.source.DocumentSource

/**
 * A helper object to generate rivers and indexes for elasticsearch using elastic4s.
 */
object IndexerElastic4s extends Indexer with RiverConfigUtil {

  val e4sClient = ElasticSearchClient.client

  /**
   * TODO: This should only drop collectionsToSlurp, but this seemed to be nontrivial
   */
  override protected def dropAll() = e4sClient.execute {
    deleteIndex("_all")
  }

  override protected def importMapping() = e4sClient.execute {
    ContentMapping.generateCreateIndexDefinition
  }

  override protected def createRivers() = rivers.map(createRiver)

  val rivers = Seq(
    River(name = "content", typ = "content", collection = "content"),
    River(name = "standards", typ = "standard", collection = "ccstandards")
  )

  private def createRiver(river: River) = e4sClient.execute {
    index.into("_river", river.name).id("_meta").doc(
      RiverConfig(
        createRiverConfigJson(river).toString
      )
    )
  }

  case class RiverConfig(json: String) extends DocumentSource

}
