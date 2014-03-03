package org.corespring.search.indexing

import play.api.libs.json._
import scala.io.Source
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.Some
import org.corespring.platform.core.models.JsonUtil
import play.api.libs.ws.WS
import com.mongodb.casbah.MongoURI
import play.api.Play
import play.api.test.FakeApplication


/**
 * A helper object to generate rivers and indexes for elasticsearch.
 */
object Indexer extends JsonUtil {

  def fakePlay() = {
    Play.start(FakeApplication())
  }

  val mongoURI = MongoURI(play.api.Play.current.configuration.getString("mongodb.default.uri")
    .getOrElse(throw failedRequirement("mongo URI")))

  val elasticSearchHost = play.api.Play.current.configuration.getString("elasticsearch.default.host")

  val database = mongoURI.database.getOrElse(throw failedRequirement("database"))
  val mongoHosts = mongoURI.hosts match {
    case nonEmpty: Seq[String] if nonEmpty.nonEmpty => nonEmpty
    case _ => throw failedRequirement("host(s)")
  }

  val rivers = Seq(
    River(name = "content", typ = "content", collection = "content"),
    River(name = "standards", typ = "standard", collection = "ccstandards"))

  /**
   * Initializes all the rivers and indexes
   */
  def initialize() = {
    dropAll()
    importMapping()
    createRivers()
  }

  /**
   * TODO: This should only drop collectionsToSlurp, but this seemed to be nontrivial
   */
  private def dropAll() = WS.url(s"http://$elasticSearchHost/_all").delete()

  private def importMapping() = {
    val mappingJson = Source.fromFile("modules/lib/search/src/main/scala/org/corespring/serach/indexing/mapping.json").mkString
    Await.result(WS.url(s"http://$elasticSearchHost/content").put(mappingJson), Duration.Inf)
  }

 def createRivers() = rivers.map(createRiver(_))

  /**
   * Creates a River for the provided collection
   */
  private def createRiver(river: River) = {
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
  }

  private def failedRequirement(requirement: String) =
    new IllegalArgumentException(s"ElasticSearch requires $requirement")

}
