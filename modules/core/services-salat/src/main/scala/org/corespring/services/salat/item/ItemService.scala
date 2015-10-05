package org.corespring.services.salat.item

import com.mongodb.casbah
import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.dao.SalatDAOUpdateError
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ ItemStandards, Item }
import org.corespring.models.item.Item.Keys
import org.corespring.models.item.resource._
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors._
import org.corespring.{ services => interface }
import org.joda.time.DateTime

import scala.xml.Elem
import scalaz._

class ItemService(
  val dao: VersioningDao[Item, VersionedId[ObjectId]],
  assets: interface.item.ItemAssetService,
  contentCollectionService: => interface.ContentCollectionService,
  implicit val context: Context,
  archiveConfig: ArchiveConfig)
  extends interface.item.ItemService {

  protected val logger = Logger(classOf[ItemService])

  private val baseQuery = MongoDBObject("contentType" -> "item")

  override def clone(item: Item): Option[Item] = {
    val itemClone = item.cloneItem
    val result: Validation[Seq[CloneFileResult], Item] = assets.cloneStoredFiles(item, itemClone)
    logger.debug(s"clone itemId=${item.id} result=$result")
    result match {
      case Success(updatedItem) =>
        dao.save(updatedItem, createNewVersion = false)
        Some(updatedItem)
      case Failure(files) =>
        files.foreach({
          case CloneFileFailure(f, err) => err.printStackTrace()
          case _ => Unit
        })
        None
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
    logger.trace(s"function=publish, id=$id")
    val update = MongoDBObject("$set" -> MongoDBObject("published" -> true))
    val result = dao.update(id, update, false)
    result.isRight
  }

  /**
   * save a new version of the item and set published to false
   */
  override def saveNewUnpublishedVersion(id: VersionedId[ObjectId]): Option[VersionedId[ObjectId]] = {
    dao.get(id).flatMap { item =>
      val update = item.copy(published = false)
      save(update, createNewVersion = true) match {
        case Failure(_) => None
        case Success(savedId) => Some(savedId)
      }
    }
  }

  override def count(query: DBObject, fields: Option[String] = None): Long = dao.countCurrent(baseQuery ++ query)

  override def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject] = dao.findDbo(id, fields)

  override def currentVersion(id: VersionedId[ObjectId]): Long = dao.getCurrentVersion(id)

  override def find(query: DBObject, fields: DBObject = new BasicDBObject()): Stream[Item] = dao.findCurrent(baseQuery ++ query, fields).toStream

  override def findOneById(id: VersionedId[ObjectId]): Option[Item] = dao.findOneById(id)

  override def findOne(query: DBObject): Option[Item] = dao.findOneCurrent(baseQuery ++ query)

  override def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean = false): Boolean = {
    val result = dao.update(id, dbo, createNewVersion)
    result.isRight
  }

  override def purge(id: VersionedId[ObjectId]) = {
    dao.delete(id)
    Success(id)
  }

  override def addFileToPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean] = {
    val dbo = com.novus.salat.grater[StoredFile].asDBObject(file)
    val update = MongoDBObject("$addToSet" -> MongoDBObject("data.playerDefinition.files" -> dbo))
    val result = dao.update(itemId, update, false)
    logger.trace(s"function=addFileToPlayerDefinition, itemId=$itemId, docsChanged=${result}")
    Validation.fromEither(result).map(id => true)
  }

  override def addFileToPlayerDefinition(item: Item, file: StoredFile): Validation[String, Boolean] = addFileToPlayerDefinition(item.id, file)

  import org.corespring.services.salat.ValidationUtils._

  // three things occur here: 1. save the new item, 2. copy the old item's s3 files, 3. update the old item's stored files with the new s3 locations
  // TODO if any of these three things fail, the database and s3 revert back to previous state
  override def save(item: Item, createNewVersion: Boolean = false): Validation[PlatformServiceError, VersionedId[ObjectId]] = {

    logger.trace(s"function=save, createNewVersion=$createNewVersion, item=$item")

    import scala.language.implicitConversions

    implicit def toServiceError[A](e: Validation[String, A]): Validation[PlatformServiceError, A] = {
      e.fold(
        err => Failure(PlatformServiceError(err)),
        (i) => Success(i))
    }

    val savedVid = dao.save(item.copy(dateModified = Some(new DateTime())), createNewVersion).leftMap(e => GeneralError(e, None))

    if (createNewVersion) {
      val newItem = dao.findOneById(VersionedId(item.id.id)).get
      val result: Validation[Seq[CloneFileResult], Item] = assets.cloneStoredFiles(item, newItem)
      result match {
        case Success(updatedItem) => dao.save(updatedItem, createNewVersion = false).leftMap(e => GeneralError(e, None))
        case Failure(files) =>
          dao.revertToVersion(item.id)
          files.foreach {
            case CloneFileSuccess(f, key) => assets.delete(key)
            case _ => Unit
          }
          Failure(PlatformServiceError("Cloning of files failed"))
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

  override def addCollectionIdToSharedCollections(itemIds: Seq[VersionedId[ObjectId]], collectionId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
    itemIds.filterNot { vid =>
      try {
        val update = MongoDBObject("$addToSet" -> MongoDBObject(Keys.sharedInCollections -> collectionId))
        dao.update(vid, update, createNewVersion = false)
        true
      } catch {
        case e: SalatDAOUpdateError => false
      }
    } match {
      case Nil => Success(itemIds)
      case failedItems => Failure(ItemShareError(failedItems, collectionId))
    }
  }


  override def removeCollectionIdsFromShared(itemIds: Seq[VersionedId[ObjectId]], collectionIds: Seq[ObjectId]): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
    itemIds.filterNot { vid =>
      try {
        dao.update(vid, MongoDBObject("$pullAll" -> MongoDBObject(Keys.sharedInCollections -> collectionIds)), createNewVersion = false)
        true
      } catch {
        case e: SalatDAOUpdateError => false
      }
    } match {
      case Nil => Success(itemIds)
      case failedItems => Failure(ItemUnShareError(failedItems, collectionIds))
    }
  }

  override def getQtiXml(id: VersionedId[ObjectId]): Option[Elem] = {
    None
  }

  override def moveItemToArchive(id: VersionedId[ObjectId]) = {
    val update = MongoDBObject("$set" -> MongoDBObject(Item.Keys.collectionId -> archiveConfig.contentCollectionId.toString))
    saveUsingDbo(id, update, createNewVersion = false)
    Some(archiveConfig.contentCollectionId.toString)
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
   * @return
   */
  override def deleteFromSharedCollections(collectionId: ObjectId): Validation[PlatformServiceError, Unit] = {
    try {
      val query = MongoDBObject(Keys.sharedInCollections -> collectionId)
      val update = MongoDBObject("$pull" -> MongoDBObject(Keys.sharedInCollections -> collectionId))
      dao.update(query, update, upsert = false, multi = true) match {
        case Left(e) => Failure(PlatformServiceError(e))
        case Right(wr) =>
          if (wr.getLastError.ok) Success() else Failure(PlatformServiceError(s"error deleting from sharedCollections in item, collectionId: $collectionId"))
      }
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError(e.getMessage))
    }
  }

  override def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Validation[PlatformServiceError, Unit] = {
    dao.findDbo(contentId, MongoDBObject("collectionId" -> 1)).map { dbo =>
      val collectionId = dbo.get("collectionId").asInstanceOf[String]
      if (ObjectId.isValid(collectionId)) {
        contentCollectionService.isAuthorized(orgId, new ObjectId(collectionId), p)
      } else {
        logger.error(s"item: $contentId has an invalid collectionId: $collectionId")
        Failure(ItemNotFoundError(orgId, p, contentId))
      }
    }.getOrElse {
      logger.debug("isAuthorized: can't find item with id: " + contentId)
      Failure(ItemNotFoundError(orgId, p, contentId))
    }
  }

  override def sessionCount(id: VersionedId[Imports.ObjectId]): Long = {
    throw new RuntimeException("not supported")
  }

  override def contributorsForOrg(orgId: ObjectId): Seq[String] = {

    val readableCollectionIds = contentCollectionService
      .getCollectionIds(orgId, Permission.Read)
      .filterNot(_ == archiveConfig.contentCollectionId)
      .map(_.toString)

    logger.trace(s"function=contributorsForOrg readableCollectionIds=$readableCollectionIds")

    val filter = MongoDBObject(
      "contentType" -> "item",
      "collectionId" -> MongoDBObject("$in" -> readableCollectionIds))
    //TODO: RF - include versioned content?

    logger.trace(s"distinct.filter=$filter")
    dao.distinct("contributorDetails.contributor", filter).toSeq.map(_.toString)
  }

  override def countItemsInCollection(collectionId: Imports.ObjectId): Long = {
    dao.countCurrent(MongoDBObject("collectionId" -> collectionId.toString))
  }

  override def collectionIdForItem(itemId: VersionedId[Imports.ObjectId]): Option[Imports.ObjectId] = {
    dao.findDbo(itemId.copy(version = None),
      MongoDBObject("collectionId" -> 1)).flatMap { dbo =>
        try {
          val idString = dbo.get("collectionId").asInstanceOf[String]
          Some(new ObjectId(idString))
        } catch {
          case t: Throwable =>
            if (logger.isDebugEnabled) {
              t.printStackTrace()
            }
            logger.error(t.getMessage)
            None
        }
      }
  }

  override def findItemStandards(itemId: VersionedId[Imports.ObjectId]): Option[ItemStandards] = {
    val fields = MongoDBObject("taskInfo.title" -> 1, "standards" -> 1)
    for {
      dbo <- dao.findDbo(itemId, fields)
      _ <- Some(logger.debug(s"function=findItemStandards, dbo=$dbo"))
      title <- dbo.expand[String]("taskInfo.title")
      _ <- Some(logger.trace(s"function=findItemStandards, title=$title"))
      standards <- dbo.expand[Seq[String]]("standards")
      _ <- Some(logger.trace(s"function=findItemStandards, standards=$standards"))
    } yield ItemStandards(title, standards, itemId)
  }
}
