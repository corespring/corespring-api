package org.corespring.search.elasticsearch.indexing

import org.corespring.platform.core.models.JsonUtil
import com.sksamuel.elastic4s.ElasticDsl._
import org.corespring.search.elasticsearch.ElasticSearchClient
import play.api.Logger

/**
 * A helper object to generate rivers and indexes for elasticsearch using elastic4s.
 */
object IndexerElastic4s extends Indexer with RiverService with JsonUtil {

  lazy val logger = Logger("IndexerElastic4s")

  val e4sClient = ElasticSearchClient.client

  /**
   * TODO: This should only drop collectionsToSlurp, but this seemed to be nontrivial
   */
  override protected def dropAll() = {
    logger.info("dropAll")
    /*
    e4sClient.execute {
      deleteIndex("_all")
    }
    */
  }

  override protected def importMapping() = {
    logger.info("importMapping")
    /*
    e4sClient.execute {
      ContentMapping.generateCreateIndexDefinition
    }
    */
  }

  override protected def createRivers() = {
    logger.info("createRivers")
    /*
    rivers.map(e4sCreateRiver)
    */
  }

  private def e4sCreateRiver( river: River ) = {
    logger.info(s"createRiver ${river.name}")
    //A river is a type in the index named _river
    //_meta is a special document in that river
    //In theory it can be passed to the server by indexing it
    //I couldn't get it to work though.
    //e4sClient.execute { createRiverDefinition(river).build }
  }

  val rivers = Seq(
    //River(name = "content", typ = "content", collection = "content"),
    //River(name = "standards", typ = "standard", collection = "ccstandards")
  )

}
