package bootstrap

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Logger

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Await, Future }
import scalaz.{ Failure, Success, Validation }

class ItemIndexingDao(
  val db: MongoDB,
  val context: Context,
  val collectionName: String,
  itemIndexService: ItemIndexService,
  implicit val executionContext: ExecutionContext) extends SalatVersioningDao[Item] {

  private val logger = Logger(classOf[ItemIndexingDao])

  override protected implicit def entityManifest: Manifest[Item] = Manifest.classType(classOf[Item])

  override def checkCurrentCollectionIntegrity: Boolean = false

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

  override def update(id: VersionedId[ObjectId], query: DBObject, multi: Boolean) = index {
    logger.trace(s"function=update")
    super.update(id, query, multi)
  }

  override def revertToVersion(id: VersionedId[ObjectId]): VersionedId[ObjectId] = index {
    logger.trace(s"function=revertToVersion")
    super.revertToVersion(id)
  }

  override def save(item: Item, createNewVersion: Boolean) = index {
    logger.trace(s"function=save")
    super.save(item, createNewVersion)
  }

  override def insert(item: Item): Option[VersionedId[ObjectId]] = index {
    logger.trace("function=insert")
    super.insert(item)
  }

}

