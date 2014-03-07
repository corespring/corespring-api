package org.corespring.search.elasticsearch

import org.elasticsearch.common.settings.ImmutableSettings
import com.sksamuel.elastic4s.ElasticClient

/**
 * A helper Object to configure the ElasticSearch client from application configuration parameters.
 */
object ElasticSearchClient extends ElasticSearchConfig {

  val settings = ImmutableSettings.settingsBuilder()
    .put("cluster.name", elasticSearchClusterName)
    .build()

  val Array(host, port) = elasticSearchHost.split(":")

  lazy val client = ElasticClient.remote(settings, (host, port.toInt))

}