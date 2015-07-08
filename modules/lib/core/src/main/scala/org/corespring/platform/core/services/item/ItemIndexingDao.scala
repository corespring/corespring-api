package org.corespring.platform.core.services.item

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Logger

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future, Await }
import scalaz.{ Failure, Success, Validation }

class ItemIndexingDao(dao: SalatVersioningDao[Item], itemIndexService: ItemIndexService)(implicit executionContext: ExecutionContext) {

  private val logger = Logger(classOf[ItemIndexingDao])

  private val collection = dao.currentCollection

  /**
   * Used for operations such as cloning and deleting, where we want the index to be updated synchronously. This is
   * needed so that the client can be assured that when they re-query the index after update that the changes will be
   * available in search results.
   */
  private def synchronousReindex(id: VersionedId[ObjectId]): Validation[Error, String] = {
    try {
      Await.result(itemIndexService.reindex(id).flatMap(result => {
        result match {
          case Success(anything) => itemIndexService.refresh()
          case Failure(error) => Future {
            Failure(error)
          }
        }
      }), Duration(20, SECONDS))
    } catch {
      case e: Exception => Failure(new Error(e.getMessage))
    }
  }

  private def index(block: => VersionedId[ObjectId]): VersionedId[ObjectId] = {
    val id = block
    synchronousReindex(id)
    id
  }

  private def index(block: => Either[String, VersionedId[ObjectId]]) = {
    val maybeId = block
    maybeId match {
      case Right(id) => synchronousReindex(id)
      case _ => logger.error("Cannot index failure")
    }
    maybeId
  }

  private def index(block: => Option[VersionedId[ObjectId]]) = {
    val maybeId = block
    maybeId.map(synchronousReindex)
    maybeId
  }

  private def vidToDbo(vid: VersionedId[ObjectId]): DBObject = {
    val base = MongoDBObject("_id._id" -> vid.id)
    vid.version.map { v =>
      base ++ MongoDBObject("_id.version" -> v)
    }.getOrElse(base)
  }

  /**
   * Indexing operations
   */

  def collectionUpdate[A, B](id: VersionedId[ObjectId], query: A, obj: B, upsert: Boolean = false,
    multi: Boolean = false, concern: com.mongodb.WriteConcern = collection.writeConcern)(implicit queryView: A => DBObject, objView: B => DBObject,
      encoder: DBEncoder = collection.customEncoderFactory.map(_.create).orNull): WriteResult = {
    val rv = collection.update(query, obj, upsert, multi)
    synchronousReindex(id)
    rv
  }

  def update(id: VersionedId[ObjectId], query: DBObject, multi: Boolean) = index { dao.update(id, query, multi) }

  def revertToVersion(id: VersionedId[ObjectId]): VersionedId[ObjectId] = index { dao.revertToVersion(id) }

  def save(item: Item, createNewVersion: Boolean) = index { dao.save(item, createNewVersion) }

  def insert(item: Item): Option[VersionedId[ObjectId]] = index { dao.insert(item) }

  /**
   * Non-indexing operations
   */
  def findOneById(id: VersionedId[ObjectId]) = dao.findOneById(id)

  def findOneCurrent[A <% DBObject](query: A) = dao.findOneCurrent(query)

  /**
   * This doesn't need to update the index, because this method is not used by any code outside of test helpers. A
   * delete in the sense of the CoreSpring API will move to the archived collection, which is handled by a dao update,
   * and therefore is reflected in an update call's reindex.
   */
  def delete(id: VersionedId[ObjectId]) = dao.delete(id)

}

