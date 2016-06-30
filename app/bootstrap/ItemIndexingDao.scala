package bootstrap

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatMongoCursor
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item.Item
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Logger

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

/**
 * TODO: It would be more appropriate to decorate <ItemService> than the dao.
 * As it would no longer be bound to a mongo based dao.
 * @param underlying
 * @param itemIndexService
 * @param executionContext
 */
class ItemIndexingDao(
  val underlying: VersioningDao[Item, VersionedId[ObjectId]],
  itemIndexService: ItemIndexService,
  implicit val executionContext: ExecutionContext) extends VersioningDao[Item, VersionedId[ObjectId]] {

  private val logger = Logger(classOf[ItemIndexingDao])

  override def update(query: DBObject, update: DBObject, upsert: Boolean, multi: Boolean): Either[String, WriteResult] = {
    logger.trace(s"function=update")
    underlying.update(query, update, upsert, multi)
  }

  override def update(id: VersionedId[ObjectId], query: DBObject, multi: Boolean) = index {
    logger.trace(s"function=update")
    underlying.update(id, query, multi)
  }

  override def revertToVersion(id: VersionedId[ObjectId]): VersionedId[ObjectId] = index {
    logger.trace(s"function=revertToVersion")
    underlying.revertToVersion(id)
  }

  override def save(item: Item, createNewVersion: Boolean) = index {
    logger.trace(s"function=save")
    underlying.save(item, createNewVersion)
  }

  override def insert(item: Item): Option[VersionedId[ObjectId]] = index {
    logger.trace("function=insert")
    underlying.insert(item)
  }

  override def get(id: VersionedId[Imports.ObjectId]): Option[Item] = {
    underlying.get(id)
  }

  override def findDbo(id: VersionedId[Imports.ObjectId], fields: DBObject): Option[DBObject] = {
    underlying.findDbo(id)
  }

  override def findDbos(ids: Seq[VersionedId[ObjectId]], fields: DBObject): Stream[DBObject] = {
    underlying.findDbos(ids, fields)
  }

  override def getCurrentVersion(id: VersionedId[Imports.ObjectId]): Long = {
    underlying.getCurrentVersion(id)
  }

  override def findCurrent[A, B](ref: A, keys: B)(implicit evidence$1: (A) => DBObject, evidence$2: (B) => DBObject): SalatMongoCursor[Item] = {
    underlying.findCurrent(ref, keys)
  }

  override def findOneById(id: VersionedId[Imports.ObjectId]): Option[Item] = {
    underlying.findOneById(id)
  }

  override def countCurrent(q: DBObject, fieldsThatMustExist: List[String], fieldsThatMustNotExist: List[String]): Long = {
    underlying.countCurrent(q, fieldsThatMustExist)
  }

  override def delete(id: VersionedId[Imports.ObjectId]): Boolean = {
    underlying.delete(id)
  }

  override def findOneCurrent[A](query: A)(implicit evidence$3: (A) => DBObject): Option[Item] = {
    underlying.findOneCurrent(query)(evidence$3)
  }

  override def list: SalatMongoCursor[Item] = {
    underlying.list
  }

  override def distinct(key: String, query: DBObject): mutable.Buffer[_] = {
    underlying.distinct(key, query)
  }

  /**
   * Used for operations such as cloning and deleting, where we want the index to be updated synchronously. This is
   * needed so that the client can be assured that when they re-query the index after update that the changes will be
   * available in search results.
   */
  private def synchronousReindex(id: VersionedId[ObjectId]): Validation[Error, String] = {

    logger.trace(s"function=synchronousReindex, id=$id")

    try {
      Await.result(itemIndexService.reindex(id).flatMap(result => {
        result match {
          case Success(anything) => itemIndexService.refresh()
          case Failure(error) => Future {
            logger.error(s"function=synchronousReindex, id=$id, error=$error")
            if (logger.isErrorEnabled) {
              error.printStackTrace()
            }
            Failure(error)
          }
        }
      }), Duration(20, SECONDS))
    } catch {
      case e: Exception => {
        logger.error(s"function=synchronousReindex, id=$id, e=$e")
        if (logger.isErrorEnabled) {
          e.printStackTrace()
        }
        Failure(new Error(e.getMessage))
      }
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

}

