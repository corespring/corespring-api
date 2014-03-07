package org.corespring.search.elasticsearch

import com.mongodb.casbah.MongoURI
import play.api.libs.ws.WS

trait ElasticSearchConfig {

  val elasticSearchHost = getConfig("elasticsearch.host")
    .getOrElse(throw failedRequirement("elastic search host"))

  val elasticSearchWebserviceHost = getConfig("elasticsearch.webservice.host")
    .getOrElse(throw failedRequirement("elastic search webservice host"))

  val elasticSearchClusterName = getConfig("elasticsearch.cluster_name")
    .getOrElse(throw failedRequirement("elasticsearch.cluster_name not found"))

  val mongoURI = MongoURI(getConfig("mongodb.default.uri")
    .getOrElse(throw failedRequirement("mongo URI")))

  val database = mongoURI.database
    .getOrElse(throw failedRequirement("database"))

  val mongoHosts = mongoURI.hosts match {
    case nonEmpty: Seq[String] if nonEmpty.nonEmpty => nonEmpty
    case _ => throw failedRequirement("host(s)")
  }

  def mongoCredentials() = (mongoURI.username, mongoURI.password) match {
    case (Some(username), Some(password)) => (Some(username), Some(password))
    case (Some(username), None) => throw failedRequirement("password")
    case (None, Some(password)) => throw failedRequirement("username")
    case (None,None) => (None,None)
  }

  def elasticSearchWs(target:String) = WS.url(s"http://$elasticSearchWebserviceHost/$target")

  private def getConfig(name: String) =
    play.api.Play.current.configuration.getString(name)

  private def failedRequirement(requirement: String): Throwable =
    new IllegalArgumentException(s"ElasticSearch requires $requirement")

}
