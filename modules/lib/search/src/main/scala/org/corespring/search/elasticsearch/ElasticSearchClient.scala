package org.corespring.search.elasticsearch

import org.elasticsearch.common.settings.ImmutableSettings
import com.sksamuel.elastic4s.ElasticClient

/**
 * A helper Object to configure the ElasticSearch client from application configuration parameters.
 */
object ElasticSearchClient {

  import play.api.Play._

  val Array(host, port) = current.configuration.getString("elasticsearch.host")
    .getOrElse(throw new IllegalStateException("elasticsearch.host not found")).split(":")

  val clusterName = current.configuration.getString("elasticsearch.cluster_name")
    .getOrElse(throw new IllegalStateException("elasticsearch.cluster_name not found"))

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build()

  lazy val client = ElasticClient.remote(settings, (host, port.toInt))

}