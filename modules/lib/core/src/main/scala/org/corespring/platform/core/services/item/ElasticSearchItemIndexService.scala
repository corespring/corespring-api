package org.corespring.platform.core.services.item

import java.net.URL

import com.mongodb.casbah.commons.MongoDBObject
import org.apache.commons.codec.binary.Base64
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.models.item.index.ItemIndexSearchResult
import org.corespring.elasticsearch._
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._
import play.api.libs.ws.WS
import scala.concurrent._
import scalaz._

/**
 * An ItemIndexService based on Elastic Search.
 *
 * TODO: Testing this is *very* difficult, as mocking the native WS in Play < 2.3.x requires starting a Mock HTTP
 * service. When we upgrade ot Play 2.3.x, we should use [play-mockws](https://github.com/leanovate/play-mockws) to
 * test this exhaustively.
 */
class ElasticSearchItemIndexService(elasticSearchUrl: URL)(implicit ec: ExecutionContext, application: play.api.Application)
    extends ItemIndexService {

  import Base64._

  val components = new ComponentMap(application)

  private val contentIndex = ElasticSearchClient(elasticSearchUrl).index("content")

  private val authHeader = Option(elasticSearchUrl.getUserInfo).map(_.split(":")) match {
    case Some(Array(username, password)) =>
      Some("Authorization" -> s"Basic ${new String(encodeBase64(s"$username:$password".getBytes))}")
    case _ => None
  }

  val baseUrl = s"${elasticSearchUrl.getProtocol}://${elasticSearchUrl.getHost}"

  private def authed(route: String) = {
    val holder = WS.url(s"$baseUrl$route")
    authHeader.map(header => holder.withHeaders(header)).getOrElse(holder)
  }

  def search(query: ItemIndexQuery): Future[Validation[Error, ItemIndexSearchResult]] = {
    try {
      implicit val QueryWrites = ItemIndexQuery.ElasticSearchWrites
      implicit val ItemIndexSearchResultFormat = ItemIndexSearchResult.Format

      authed("/content/_search")
        .post(Json.toJson(query))
        .map(result => Json.fromJson[ItemIndexSearchResult](Json.parse(result.body)) match {
          case JsSuccess(searchResult, _) => Success(searchResult)
          case _ => Failure(new Error("Could not read results"))
        })
    } catch {
      case e: Exception => future { Failure(new Error(e.getMessage)) }
    }
  }

  def distinct(field: String): Future[Validation[Error, Seq[String]]] = {
    try {
      implicit val AggregationWrites = ItemIndexAggregation.Writes
      val agg = ItemIndexAggregation(field = field)
      authed("/content/_search")
        .post(Json.toJson(agg))
        .map(result => {
          Success((Json.parse(result.body) \ "aggregations" \ agg.name \ "buckets").as[Seq[JsObject]]
            .map(obj  => (obj \ "key").as[String]))
        })
    } catch {
      case e: Exception => future { Failure(new Error(e.getMessage)) }
    }
  }

  def reindex(id: VersionedId[ObjectId]) = Indexer.reindex(id)

  lazy val componentTypes: Future[Validation[Error, Map[String, String]]] =
    distinct("taskInfo.itemTypes").map(result => result.map(itemTypes => itemTypes.map(itemType =>
      components.componentMap.get(itemType).map(t => t.nonEmpty match {
        case true => Some(t -> itemType)
        case _ => None
      }).flatten
    ).flatten.toMap))

  /**
   * TODO: Big tech debt. This *must* be replaced with a rabbitmq/amqp solution.
   */
  private object Indexer {

    val contentDenormalizer =
      new ContentDenormalizer(play.api.Play.current.configuration
        .getConfig("mongodb").map(_.getConfig("default")).flatten.map(_.getString("uri")).flatten
        .getOrElse(throw new Exception("Cannot connect to MongoDB without URI")))


    def reindex(id: VersionedId[ObjectId]): Future[Validation[Error, String]] = {
      contentDenormalizer.withCollection("content", collection => {
        collection.findOne(MongoDBObject("_id._id" -> id.id), ContentIndexer.defaultFilter) match {
          case Some(record) => {
            contentIndex.add(id.id.toString, contentDenormalizer.denormalize(Json.parse(record.toString)).toString)
          }
          case _ => future { Failure(new Error(s"Item with id=$id not found")) }
        }
      })
    }
  }

}

object ElasticSearchItemIndexService extends
    ElasticSearchItemIndexService(AppConfig.elasticSearchUrl)(ExecutionContext.Implicits.global, play.api.Play.current)