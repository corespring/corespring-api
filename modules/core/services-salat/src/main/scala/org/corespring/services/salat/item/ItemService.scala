package org.corespring.services.salat.item

import com.mongodb.casbah
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{ MongoDBObject }
import com.novus.salat._
import com.novus.salat.dao.{ SalatDAOUpdateError }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.models.item.Item.Keys
import org.corespring.models.item.resource._
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError
import org.joda.time.DateTime
import scala.concurrent.{ ExecutionContext }
import scala.xml.Elem
import scalaz._
import org.corespring.{ services => interface }

trait ItemService
  extends interface.item.ItemService
  with interface.item.ItemPublishingService {

  protected val logger = Logger(classOf[ItemService])

  private val baseQuery = MongoDBObject("contentType" -> "item")

  def dao: SalatVersioningDao[Item]

  def assets: interface.item.ItemAssetService
  def contentCollectionService: interface.ContentCollectionService

  implicit def context: Context

  private lazy val collection = dao.currentCollection

  def archiveCollectionId: ObjectId

  /**
   * Used for operations such as cloning and deleting, where we want the index to be updated synchronously. This is
   * needed so that the client can be assured that when they re-query the index after update that the changes will be
   * available in search results.
   * TODO: RF: To be removed - outside the scope of this library
   */
  private def syncronousReindex(id: VersionedId[ObjectId]): Validation[Error, String] = {
    Success("")
  }

  override def clone(item: Item): Option[Item] = {
    val itemClone = item.cloneItem
    val result: Validation[Seq[CloneFileResult], Item] = assets.cloneStoredFiles(item, itemClone)
    logger.debug(s"clone itemId=${item.id} result=$result")
    result match {
      case Success(updatedItem) => {
        dao.save(updatedItem, false)
        syncronousReindex(updatedItem.id)
        Some(updatedItem)
      }
      case Failure(files) => {
        files.foreach({
          case CloneFileFailure(f, err) => err.printStackTrace
          case _ => Unit
        })
        None
      }
    }
  }

  def vidToDbo(vid: VersionedId[ObjectId]): DBObject = {
    val base = MongoDBObject("_id._id" -> vid.id)
    vid.version.map { v =>
      base ++ MongoDBObject("_id.version" -> v)
    }.getOrElse(base)
  }

  override def asMetadataOnly(i: Item): DBObject = {
    import com.mongodb.casbah.commons.MongoDBObject
    import com.novus.salat._
    val timestamped = i.copy(dateModified = Some(new DateTime()))
    val dbo: MongoDBObject = new MongoDBObject(grater[Item].asDBObject(timestamped))
    dbo - "_id" - Keys.supportingMaterials - Keys.data - Keys.collectionId
  }

  override def publish(id: VersionedId[ObjectId]): Boolean = {
    val update = MongoDBObject("$set" -> MongoDBObject("published" -> true))
    val result = collection.update(vidToDbo(id), update, false)
    syncronousReindex(id)
    result.getLastError.ok
  }

  /**
   * save a new version of the item and set published to false
   * @param id
   * @return the VersionedId[ObjectId] of the new item
   */
  override def saveNewUnpublishedVersion(id: VersionedId[ObjectId]): Option[VersionedId[ObjectId]] = {
    dao.get(id).map { item =>
      val update = item.copy(published = false)
      save(update, true) match {
        case Left(_) => None
        case Right(id) => Some(id)
      }
    }.flatten
  }

  def count(query: DBObject, fields: Option[String] = None): Int = dao.countCurrent(baseQuery ++ query).toInt

  def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject] = dao.findDbo(id, fields)

  override def currentVersion(id: VersionedId[ObjectId]): Long = dao.getCurrentVersion(id)

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): Stream[Item] = dao.findCurrent(baseQuery ++ query, fields).toStream

  def findOneById(id: VersionedId[ObjectId]): Option[Item] = dao.findOneById(id)

  def findOne(query: DBObject): Option[Item] = dao.findOneCurrent(baseQuery ++ query)

  def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean = false): Boolean = {
    val result = dao.update(id, dbo, createNewVersion)
    syncronousReindex(id)
    result.isRight
  }

  def deleteUsingDao(id: VersionedId[ObjectId]) = dao.delete(id)

  override def addFileToPlayerDefinition(item: Item, file: StoredFile): Validation[String, Boolean] = {
    val dbo = com.novus.salat.grater[StoredFile].asDBObject(file)
    val update = MongoDBObject("$addToSet" -> MongoDBObject("data.playerDefinition.files" -> dbo))
    val result = collection.update(MongoDBObject("_id._id" -> item.id.id), update, false, false)
    logger.trace(s"function=addFileToPlayerDefinition, itemId=${item.id}, docsChanged=${result.getN}")
    require(result.getN == 1, s"Exactly 1 document with id: ${item.id} must have been updated")
    if (result.getN != 1) {
      Failure(s"Wrong number of documents updated for ${item.id}")
    } else {
      Success(result.getN == 1)
    }
  }

  // three things occur here: 1. save the new item, 2. copy the old item's s3 files, 3. update the old item's stored files with the new s3 locations
  // TODO if any of these three things fail, the database and s3 revert back to previous state
  override def save(item: Item, createNewVersion: Boolean = false): Either[PlatformServiceError, VersionedId[ObjectId]] = {

    import scala.language.implicitConversions

    implicit def toServiceError[A](e: Either[String, A]): Either[PlatformServiceError, A] = {
      e.fold(
        err => Left(PlatformServiceError(err)),
        (i) => Right(i))
    }

    val savedVid = dao.save(item.copy(dateModified = Some(new DateTime())), createNewVersion)

    if (createNewVersion) {
      val newItem = dao.findOneById(VersionedId(item.id.id)).get
      val result: Validation[Seq[CloneFileResult], Item] = assets.cloneStoredFiles(item, newItem)
      result match {
        case Success(updatedItem) => {
          dao.save(updatedItem, false)
        }
        case Failure(files) => {
          dao.revertToVersion(item.id)
          files.foreach {
            case CloneFileSuccess(f, key) => {
              assets.delete(key)
            }
            case _ => Unit
          }
          Left(PlatformServiceError("Cloning of files failed"))
        }
      }
    } else {
      savedVid
    }
  }

  def insert(i: Item): Option[VersionedId[ObjectId]] = dao.insert(i)

  def findMultiple(ids: Seq[VersionedId[ObjectId]], keys: DBObject): Seq[Item] = {
    val oids = ids.map(i => i.id)
    val query = baseQuery ++ MongoDBObject("_id._id" -> MongoDBObject("$in" -> oids))
    val out = dao.findCurrent(query, keys).toSeq
    out
  }

  override def addCollectionIdToSharedCollections(itemId: VersionedId[ObjectId], collectionId: ObjectId): Either[PlatformServiceError, Unit] = try {
    val update = MongoDBObject("$addToSet" -> MongoDBObject(Keys.sharedInCollections -> collectionId))
    Right(dao.update(itemId, update, false))
  } catch {
    case e: SalatDAOUpdateError => Left(PlatformServiceError(s"Error adding collectionId $collectionId for item with id $itemId"))
  }

  override def removeCollectionIdsFromShared(itemIds: Seq[VersionedId[ObjectId]], collectionIds: Seq[ObjectId]): Either[Seq[VersionedId[ObjectId]], Unit] = {

    val failedItems = itemIds.filterNot { vid =>
      try {
        dao.update(vid, MongoDBObject("$pullAll" -> MongoDBObject(Keys.sharedInCollections -> collectionIds)), false)
        true
      } catch {
        case e: SalatDAOUpdateError => false
      }
    }
    if (failedItems.size > 0) {
      Left(failedItems)
    } else {
      Right(Unit)
    }
  }

  override def getQtiXml(id: VersionedId[ObjectId]): Option[Elem] = {
    None
  }

  override def moveItemToArchive(id: VersionedId[ObjectId]) = {
    val update = MongoDBObject("$set" -> MongoDBObject(Item.Keys.collectionId -> archiveCollectionId.toString))
    saveUsingDbo(id, update, false)
    Some(archiveCollectionId.toString)
  }

  override def isPublished(vid: VersionedId[casbah.Imports.ObjectId]): Boolean = {
    val dbo = vidToDbo(vid) ++ MongoDBObject("published" -> true)
    count(dbo) == 1
  }

  override def getOrCreateUnpublishedVersion(id: VersionedId[ObjectId]): Option[Item] = {
    val unpublished = dao.findOneCurrent(MongoDBObject("_id._id" -> id.id, "published" -> false))
    unpublished.orElse {
      saveNewUnpublishedVersion(id).flatMap(vid => findOneById(vid))
    }
  }

  override def findMultipleById(ids: ObjectId*): Stream[Item] = {
    dao.findCurrent(MongoDBObject("_id._id" -> MongoDBObject("$in" -> ids)), MongoDBObject()).toStream
  }

  /**
   * Delete collection reference from shared collections (defined in items)
   * @param collectionId
   * @return
   */
  override def deleteFromSharedCollections(collectionId: ObjectId): Either[PlatformServiceError, Unit] = {
    try {
      val query = MongoDBObject(Keys.sharedInCollections -> collectionId)
      val update = MongoDBObject("$pull" -> MongoDBObject(Keys.sharedInCollections -> collectionId))
      val result = dao.currentCollection.update(query, update, false, true)
      if (result.getLastError.ok) Right() else Left(PlatformServiceError(s"error deleting from sharedCollections in item, collectionId: $collectionId"))
    } catch {
      case e: SalatDAOUpdateError => Left(PlatformServiceError(e.getMessage))
    }
  }

  override def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Boolean = {
    dao.findDbo(contentId, MongoDBObject("collectionId" -> 1)).map { dbo =>
      val collectionId = dbo.get("collectionId").asInstanceOf[String]
      if (ObjectId.isValid(collectionId)) {
        contentCollectionService.isAuthorized(orgId, new ObjectId(collectionId), p)
      } else {
        logger.error(s"item: $contentId has an invalid collectionId: $collectionId")
        false
      }
    }.getOrElse {
      logger.debug("isAuthorized: can't find item with id: " + contentId)
      false
    }
  }
}
