package org.corespring.itemSearch

import java.net.URL

import com.mongodb.casbah.commons.MongoDBObject
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.elasticsearch._
import org.corespring.models.item.ComponentType
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._
import play.api.libs.json.Json._

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
class ElasticSearchItemIndexService(
  contentDenormalizer: ContentDenormalizer,
  rawTypes: Seq[ComponentType],
  contentIndex: ContentIndex,
  executionContext: ElasticSearchExecutionContext)
  extends ItemIndexService with ItemIndexDeleteService {

  implicit val ec: ExecutionContext = executionContext.context

  private val logger = Logger(classOf[ElasticSearchItemIndexService])

  override def unboundedSearch(query: ItemIndexQuery): Future[Validation[Error, ItemIndexSearchResult]] = {
    try {

      implicit val QueryWrites = ItemIndexQuery.ElasticSearchWrites
      implicit val ItemIndexSearchResultFormat = ItemIndexSearchResult.Format

      val queryJson = Json.toJson(query)
      logger.trace(s"function=unboundedSearch\n\tquery=$query\n\tqueryJson=${Json.prettyPrint(queryJson)}")

      contentIndex.search(queryJson)
        .map { result =>
          val resultJson = Json.parse(result.body)
          logger.trace(s"function=unboundedSearch, resultJson=${Json.prettyPrint(resultJson)}")
          Json.fromJson[ItemIndexSearchResult](resultJson) match {
            case JsSuccess(searchResult, _) => {
              logger.trace(s"function=unboundedSearch, searchResult=$searchResult")
              Success(searchResult)
            }
            case _ => Failure(new Error("Could not read results"))
          }
        }
    } catch {
      case e: Exception => future { Failure(new Error(e.getMessage)) }
    }
  }

  def search(query: ItemIndexQuery): Future[Validation[Error, ItemIndexSearchResult]] = query.collections match {
    case Nil => Future(Success(ItemIndexSearchResult(total = 0, hits = Seq.empty)))
    case _ => unboundedSearch(query)
  }

  def distinct(field: String, collectionIds: Seq[String]): Future[Validation[Error, Seq[String]]] = {
    try {

      logger.trace(s"function=distinct, field=$field")
      implicit val AggregationWrites = ItemIndexAggregation.Writes
      val agg = ItemIndexAggregation(field = field, collectionIds = collectionIds)
      val searchQuery = Json.toJson(agg)
      logger.trace(s"function=distinct, field=$field, searchQuery=$searchQuery")

      contentIndex.search(searchQuery)
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
      if (logger.isDebugEnabled) {
        error.printStackTrace()
      }
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

  override def componentTypes: Future[Validation[Error, Map[String, String]]] = getTypes("taskInfo.itemTypes")
  override def widgetTypes: Future[Validation[Error, Map[String, String]]] = getTypes("taskInfo.widgets")

  /**
   * TODO: Big tech debt. This *must* be replaced with a rabbitmq/amqp solution.
   */
  private object Indexer {

    def reindex(id: VersionedId[ObjectId]): Future[Validation[Error, String]] = {

      logger.debug(s"function=reindex, id=$id")
      if (id.version.isEmpty) {
        Future.successful(Failure(new Error(s"id is missing a version - can't index: $id")))
      } else {
        for {
          maybeDbo <- Future {
            contentDenormalizer.withCollection("content", collection => {
              collection.findOne(MongoDBObject("_id._id" -> id.id))
            })
          }
          mainDbo <- Future.successful(maybeDbo.getOrElse(Failure(new Error(s"Can't find dbo by id: $id"))))
          versionedDbo <- if (id.version.get <= 0) Future.successful(None) else Future {

            val query = MongoDBObject("_id._id" -> id.id, "_id.version" -> (id.version.get - 1))
            contentDenormalizer.withCollection("versioned_content", { c =>
              c.findOne(query)
            })
          }
          mainDenormalized <- contentDenormalizer.denormalize(Json.parse(mainDbo.toString))
          versionedDenormalized <- versionedDbo.map { v =>
            contentDenormalizer.denormalize(Json.parse(v.toString)).map(Some(_))
          }.getOrElse(Future.successful(None))
          result <- {
            logger.trace(s"function=reindex, id=$id, main=${prettyPrint(mainDenormalized.denormalized)}")
            logger.trace(s"function=reindex, id=$id, versioned=${prettyPrint(versionedDenormalized.map(_.denormalized).getOrElse(obj("empty" -> true)))}")
            contentIndex.bulkAdd(true, ItemData(mainDenormalized, versionedDenormalized))
          }
        } yield result.map(_.result).headOption.getOrElse(Failure(new Error("reindex failed")))
      }
    }
  }

  override def delete(): Future[Validation[Error, Unit]] = {
    contentIndex.dropIndex()
      .recover({ case e => Failure(new Error("failed to delete the index")) })
      .map(_ => Success())
  }

  override def create(): Future[Validation[Error, Unit]] = {
    contentIndex.recreate().map { v => v.map(_ => Unit) }
  }
}

