package org.corespring.itemSearch

import java.net.URL

import com.mongodb.casbah.commons.MongoDBObject
import grizzled.slf4j.Logger
import org.apache.commons.codec.binary.Base64._
import org.bson.types.ObjectId
import org.corespring.elasticsearch._
import org.corespring.models.item.ComponentType
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._
import play.api.libs.ws.WS

import scala.concurrent._
import scalaz._

case class ElasticSearchUrl(url: URL)
case class ElasticSearchExecutionContext(context: ExecutionContext)
case class ElasticSearchConfig(url: URL, mongoUri: String, componentPath: String)

/**
 * An ItemIndexService based on Elastic Search.
 *
 * TODO: Testing this is *very* difficult, as mocking the native WS in Play < 2.3.x requires starting a Mock HTTP
 * service. When we upgrade ot Play 2.3.x, we should use [play-mockws](https://github.com/leanovate/play-mockws) to
 * test this exhaustively.
 */
class ElasticSearchItemIndexService(config: ElasticSearchConfig,
  rawTypes: Seq[ComponentType],
  executionContext: ElasticSearchExecutionContext)
  extends ItemIndexService with AuthenticatedUrl with ItemIndexDeleteService {

  implicit def ec: ExecutionContext = executionContext.context

  private val logger = Logger(classOf[ElasticSearchItemIndexService])

  implicit val url = config.url

  private val contentIndex = ElasticSearchClient(config.url).index("content")

  def search(query: ItemIndexQuery): Future[Validation[Error, ItemIndexSearchResult]] = {
    try {
      implicit val QueryWrites = ItemIndexQuery.ElasticSearchWrites
      implicit val ItemIndexSearchResultFormat = ItemIndexSearchResult.Format

      authed("/content/_search")(url, ec)
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

      logger.trace(s"function=distinct, field=$field")
      implicit val AggregationWrites = ItemIndexAggregation.Writes
      val agg = ItemIndexAggregation(field = field, collectionIds = collectionIds)
      val searchQuery = Json.toJson(agg)
      logger.trace(s"function=distinct, field=$field, searchQuery=$searchQuery")

      authed("/content/_search")(url, ec)
        .post(searchQuery)
        .map(result => {

          val resultJson = Json.parse(result.body)
          logger.trace(s"function=distinct, field=$field, resultJson=$resultJson")

          val buckets =
            (resultJson \ "aggregations" \ agg.name \ "buckets")
              .asOpt[Seq[JsObject]]

          logger.trace(s"function=distinct, field=$field, buckets=$buckets")

          Success(buckets.map { seq =>
            seq.map(obj => (obj \ "key").as[String])
          }.getOrElse(Seq.empty))
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

  private def getTypes(key: String) = {
    val types: Future[Validation[Error, Seq[String]]] = distinct(key)

    types.map { v =>
      v.map { itemTypes =>
        logger.trace(s"function=getTypes, key=$key, types=$itemTypes")
        val comps = rawTypes.filter(c => itemTypes.contains(c.componentType))
        val out = comps.map(_.tuple).toMap
        logger.debug(s"function=getTypes, key=$key, out=$out")
        out
      }
    }
  }

  lazy val componentTypes: Future[Validation[Error, Map[String, String]]] = getTypes("taskInfo.itemTypes")
  lazy val widgetTypes: Future[Validation[Error, Map[String, String]]] = getTypes("taskInfo.widgets")

  /**
   * TODO: Big tech debt. This *must* be replaced with a rabbitmq/amqp solution.
   */
  private object Indexer {

    val contentDenormalizer = new ContentDenormalizer(config.mongoUri, config.componentPath)

    def reindex(id: VersionedId[ObjectId]): Future[Validation[Error, String]] = {
      contentDenormalizer.withCollection("content", collection => {
        collection.findOne(MongoDBObject("_id._id" -> id.id), ContentIndexer.defaultFilter) match {
          case Some(record) => {
            val recordJson = Json.parse(record.toString)
            logger.trace(s"function=reindex, id=$id, record=$recordJson")
            val denormalized = contentDenormalizer.denormalize(recordJson)
            logger.trace(s"function=reindex, id=$id, denormalized=$denormalized")
            contentIndex.add(id.id.toString, denormalized.toString)
          }
          case _ => future { Failure(new Error(s"Item with id=$id not found")) }
        }
      })
    }
  }

  override def delete(): Future[Validation[Error, Unit]] = {
    authed("/content")
      .delete()
      .recover({ case e => Failure(new Error("failed to delete the index")) })
      .map(_ => Success())
  }
}

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

