package index

import org.elasticsearch.node.NodeBuilder._
import org.elasticsearch.client._
import play.api.libs.json.Json

/**
 * A helper object to generate rivers and indexes for elasticsearch.
 */
object ElasticSearch extends UsingClient {

  import Requests._

  val mongoURI = play.api.Play.current.configuration.getString("mongodb.default.uri")
  val collectionsToSlurp = Seq("content", "ccstandards")

  def initialize = {
    run { implicit client =>
      collectionsToSlurp.foreach(createRiver(_))
    }
  }

  private def createRiver(collection: String)(implicit client: Client) = {
    val source = Json.obj(
      "type" -> "mongodb",
      "mongodb" -> Json.obj(
        "db" -> "api", // TODO: Parse from Mongo URI
        "collection" -> collection,
        "gridfs" -> false
      ),
      "index" -> Json.obj(
        "name" -> collection,
        "type" -> "documents"
      )
    )
    client.index(indexRequest("_river").`type`("_myriver").id(collection).source(source.toString)).actionGet()
  }

}

/**
 *
 */
trait UsingClient {

  def run(block: (Client => Unit)) = {
    val node = nodeBuilder.node()
    val client = node.client()
    try {
      block(client)
    } finally {
      client.close()
    }
  }

}
