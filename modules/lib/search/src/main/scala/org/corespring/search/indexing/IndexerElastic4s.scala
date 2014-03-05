package org.corespring.search.indexing

import play.api.libs.json._
import scala.io.Source
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.Some
import play.api.libs.ws.WS
import com.mongodb.casbah.MongoURI
import org.corespring.platform.core.models.JsonUtil
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.mapping._
import com.sksamuel.elastic4s.ElasticDsl._
import org.corespring.search.elasticsearch.ElasticSearchClient
import com.sksamuel.elastic4s.mapping.FieldType.StringType
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * A helper object to generate rivers and indexes for elasticsearch using elastic4s.
 */
object IndexerElastic4s extends Indexer with JsonUtil {

  val e4sClient = ElasticSearchClient.client

  /**
   * TODO: This should only drop collectionsToSlurp, but this seemed to be nontrivial
   */
  override protected def dropAll() = e4sClient.execute { deleteIndex("_all") }

  override protected def importMapping() = e4sClient.execute { ContentMapping.generate }

  override protected def createRivers() = rivers.map(createRiver(_))

  val rivers = Seq(
    River(name = "content", typ = "content", collection = "content"),
    River(name = "standards", typ = "standard", collection = "ccstandards"))

  /**
   * Creates a River for the provided collection
   */
  private def createRiver(river: River) = {
    /*
    val source = Json.obj(
      "type" -> "mongodb",
      "mongodb" -> partialObj(
        "servers" -> Some(JsArray(mongoHosts.map(host => Json.obj(
          "host" -> host.split(":").head,
          "port" -> host.split(":").last
        )))),
        "credentials" -> ((mongoURI.username, mongoURI.password) match {
          case (Some(username), Some(password)) => {
            Some(Json.obj(
              "db" -> database,
              "user" -> username,
              "password" -> password.toString
            ))
          }
          case (None, None) => None
          case _ => throw failedRequirement("username xor password")
        }),
        "db" -> Some(JsString(database)),
        "collection" -> Some(JsString(river.collection)),
        "gridfs" -> Some(JsBoolean(false))
      ),
      "index" -> Json.obj(
        "name" -> river.name,
        "type" -> river.typ
      )
    ).toString

    WS.url(s"http://$elasticSearchHost/_river/${river.name}/_meta").put(source)
    */
  }

  private def failedRequirement(requirement: String) =
    new IllegalArgumentException(s"ElasticSearch requires $requirement")

}
