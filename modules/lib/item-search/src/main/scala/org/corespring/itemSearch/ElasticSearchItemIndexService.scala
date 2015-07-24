package org.corespring.itemSearch

import java.net.URL

import com.mongodb.casbah.commons.MongoDBObject
import grizzled.slf4j.Logger
import org.apache.commons.codec.binary.Base64._
import org.bson.types.ObjectId
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
class ElasticSearchItemIndexService(elasticSearchUrl: URL, componentMap: => Map[String, String])(implicit ec: ExecutionContext)
  extends ItemIndexService with AuthenticatedUrl {

  private val logger = Logger(classOf[ElasticSearchItemIndexService])

  implicit val url = elasticSearchUrl

  private val contentIndex = ElasticSearchClient(elasticSearchUrl).index("content")

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

  def distinct(field: String, collectionIds: Seq[String]): Future[Validation[Error, Seq[String]]] = {
    try {
      implicit val AggregationWrites = ItemIndexAggregation.Writes
      val agg = ItemIndexAggregation(field = field, collectionIds = collectionIds)
      authed("/content/_search")
        .post(Json.toJson(agg))
        .map(result => {
          Success((Json.parse(result.body) \ "aggregations" \ agg.name \ "buckets").as[Seq[JsObject]]
            .map(obj => (obj \ "key").as[String]))
        })
    } catch {
      case e: Exception => future { Failure(new Error(e.getMessage)) }
    }
  }

  def reindex(id: VersionedId[ObjectId]) = Indexer.reindex(id).map(_ match {
    case Failure(error) => {
      logger.error(s"Item indexing failed: ${error.getMessage}")
      Failure(error)
    }
    case Success(message) => {
      logger.info(s"Item indexing succeeded: $message")
      Success(message)
    }
  })

  def refresh() = contentIndex.refresh()

  lazy val componentTypes: Future[Validation[Error, Map[String, String]]] =
    distinct("taskInfo.itemTypes").map(result => result.map(itemTypes => itemTypes.map(itemType =>
      componentMap.get(itemType).map(t => t.nonEmpty match {
        case true => Some(t -> itemType)
        case _ => None
      }).flatten).flatten.toMap))

  /**
   * TODO: Big tech debt. This *must* be replaced with a rabbitmq/amqp solution.
   */
  private object Indexer {

    val contentDenormalizer = {

      val cfg = play.api.Play.current.configuration
      val uri = cfg.getString("mongodb.default.uri")
      val componentPath = cfg.getString("container.components.path")

      require(uri.isDefined, "Cannot connect to MongoDB without URI")
      require(componentPath.isDefined, "Cannot use content denormalizer without component path")

      new ContentDenormalizer(uri.get, componentPath.get)
    }

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

//object ElasticSearchItemIndexService extends ElasticSearchItemIndexService(AppConfig.elasticSearchUrl)(ExecutionContext.Implicits.global, play.api.Play.current)

trait AuthenticatedUrl {

  private def baseUrl(url: URL) =
    s"${url.getProtocol}://${url.getHost}${if (url.getPort == -1) "" else s":${url.getPort}"}"

  private def authHeader(implicit url: URL) = Option(url.getUserInfo).map(_.split(":")) match {
    case Some(Array(username, password)) =>
      Some("Authorization" -> s"Basic ${new String(encodeBase64(s"$username:$password".getBytes))}")
    case _ => None
  }

  /**
   * Provides a WSRequest with authentication headers from a route and a URL-base.
   */
  def authed(route: String)(implicit url: URL, ec: ExecutionContext) = {
    val holder = WS.url(s"${baseUrl(url)}$route")
    authHeader.map(header => holder.withHeaders(header)).getOrElse(holder)
  }

}