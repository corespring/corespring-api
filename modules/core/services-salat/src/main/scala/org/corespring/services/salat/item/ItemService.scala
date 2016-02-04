package org.corespring.services.salat.item

import com.mongodb.casbah.Imports._
import com.novus.salat._
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.item.{ ItemNotFound, OrgNotAuthorized }
import org.corespring.errors.{ GeneralError, PlatformServiceError }
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item.Keys
import org.corespring.models.item.resource._
import org.corespring.models.item.{ Item, ItemStandards }
import org.corespring.mongo.IdConverters
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemCount
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.corespring.{ services => interface }
import org.joda.time.DateTime

import scala.concurrent.{ ExecutionContext, Future }
import scalaz._

class ItemService(
  val dao: VersioningDao[Item, VersionedId[ObjectId]],
  currentCollection: MongoCollection,
  assets: interface.item.ItemAssetService,
  orgCollectionService: => interface.OrgCollectionService,
  implicit val context: Context,
  archiveConfig: ArchiveConfig,
  salatServicesExecutionContext: SalatServicesExecutionContext)
  extends interface.item.ItemService with IdConverters {

  implicit val ec: ExecutionContext = salatServicesExecutionContext.ctx

  protected val logger = Logger(classOf[ItemService])

  private val baseQuery = MongoDBObject(Keys.contentType -> Item.contentType)

  override def cloneToCollection(item: Item, targetCollectionId: ObjectId): Validation[String, Item] = cloneItem(item, Some(targetCollectionId))

  override def clone(item: Item): Validation[String, Item] = cloneItem(item)

  private def cloneItem(item: Item, otherCollectionId: Option[ObjectId] = None): Validation[String, Item] = {
    val collectionId = otherCollectionId.map(_.toString).getOrElse(item.collectionId)
    val itemClone = item.cloneItem(collectionId)
    val result: Validation[CloneError, Item] = assets.cloneStoredFiles(item, itemClone)
    logger.debug(s"clone itemId=${item.id} result=$result")

    result.bimap(
      failure => {
        s"Cloning failed: ${failure.message}"
      },
      updatedItem => {
        dao.save(updatedItem, createNewVersion = false)
        updatedItem
      })
  }

  override def publish(id: VersionedId[ObjectId]): Boolean = {
    logger.trace(s"function=publish, id=$id")
    val update = MongoDBObject("$set" -> MongoDBObject(Keys.published -> true))
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

  override def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject] = dao.findDbo(id, fields)

  override def currentVersion(id: VersionedId[ObjectId]): Long = dao.getCurrentVersion(id)

  override def findOneById(id: VersionedId[ObjectId]): Option[Item] = dao.findOneById(id)

  override def purge(id: VersionedId[ObjectId]) = {
    dao.delete(id)
    Success(id)
  }

  override def addFileToPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean] = {
    val dbo = com.novus.salat.grater[StoredFile].asDBObject(file)
    //TODO It was writing to data.playerDefinition before. Is that correct?
    val update = MongoDBObject("$addToSet" -> MongoDBObject("playerDefinition.files" -> dbo))
    val result = dao.update(itemId, update, false)

    logger.trace(s"function=addFileToPlayerDefinition, itemId=$itemId, docsChanged=${result}")
    Validation.fromEither(result).map(id => true)
  }

  override def addFileToPlayerDefinition(item: Item, file: StoredFile): Validation[String, Boolean] = addFileToPlayerDefinition(item.id, file)

  override def removeFileFromPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean] = {
    val dbo = com.novus.salat.grater[StoredFile].asDBObject(file)
    val update = MongoDBObject("$pull" -> MongoDBObject("playerDefinition.files" -> dbo))
    val result = dao.update(itemId, update, false)

    logger.trace(s"function=removeFileFromPlayerDefinition, itemId=$itemId, docsChanged=${result}")
    Validation.fromEither(result).map(id => true)
  }

  import org.corespring.services.salat.ValidationUtils._

  // three things occur here:
  // 1. save the new item,
  // 2. copy the old item's s3 files,
  // 3. update the old item's stored files with the new s3 locations
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
      val result: Validation[CloneError, Item] = assets.cloneStoredFiles(item, newItem)
      logger.trace(s"function=save, cloneStoredFilesResult=$result")
      result match {
        case Success(updatedItem) => dao.save(updatedItem, createNewVersion = false).leftMap(e => GeneralError(e, None))
        case Failure(err) =>
          dao.revertToVersion(item.id)
          err match {
            case CloningFailed(failures) => {
              failures.foreach {
                case CloneFileSuccess(f, key) => assets.delete(key)
                case _ => Unit
              }
            }
            case _ => //no-op
          }
          Failure(PlatformServiceError("Cloning of files failed"))
      }
    } else {
      savedVid
    }
  }

  def insert(i: Item): Option[VersionedId[ObjectId]] = dao.insert(i)

  override def moveItemToArchive(id: VersionedId[ObjectId]) = {
    val update = MongoDBObject("$set" -> MongoDBObject(Item.Keys.collectionId -> archiveConfig.contentCollectionId.toString))
    dao.update(id, update, createNewVersion = false)
    Some(archiveConfig.contentCollectionId.toString)
  }

  override def getOrCreateUnpublishedVersion(id: VersionedId[ObjectId]): Option[Item] = {
    dao.findOneCurrent(MongoDBObject("_id._id" -> id.id, Keys.published -> false)).orElse {
      saveNewUnpublishedVersion(id).flatMap(vid => findOneById(vid))
    }
  }

  override def findMultipleById(ids: ObjectId*): Stream[Item] = {
    dao.findCurrent(MongoDBObject("_id._id" -> MongoDBObject("$in" -> ids)), MongoDBObject()).toStream
  }

  override def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Validation[PlatformServiceError, Unit] = {
    dao.findDbo(contentId, MongoDBObject(Keys.collectionId -> 1)).map { dbo =>
      val collectionId = dbo.get(Keys.collectionId).asInstanceOf[String]
      if (ObjectId.isValid(collectionId) && orgCollectionService.isAuthorized(orgId, new ObjectId(collectionId), p)) {
        Success()
      } else {
        logger.error(s"item: $contentId has an invalid collectionId: $collectionId")
        Failure(OrgNotAuthorized(orgId, p, contentId))
      }
    }.getOrElse {
      logger.debug("isAuthorized: can't find item with id: " + contentId)
      Failure(ItemNotFound(contentId))
    }
  }

  override def contributorsForOrg(orgId: ObjectId): Seq[String] = {

    val readableCollectionIds = orgCollectionService
      .getCollections(orgId, Permission.Read)
      .fold(_ => Seq.empty, c => c)
      .map(_.id)
      .filterNot(_ == archiveConfig.contentCollectionId)
      .map(_.toString)

    logger.trace(s"function=contributorsForOrg readableCollectionIds=$readableCollectionIds")

    val filter = baseQuery ++ MongoDBObject(
      Keys.collectionId -> MongoDBObject("$in" -> readableCollectionIds))
    //TODO: RF - include versioned content?

    logger.trace(s"distinct.filter=$filter")
    dao.distinct("contributorDetails.contributor", filter).toSeq.map(_.toString)
  }
  //TODO - would db("content").group be quicker?
  override def countItemsInCollections(collectionIds: ObjectId*): Future[Seq[ItemCount]] = Future {

    logger.debug(s"function=countItemsInCollections, collectionIds=$collectionIds")

    def toItemCount(dbo: DBObject): Option[ItemCount] = {
      for {
        rawId <- dbo.expand[String]("_id")
        if (ObjectId.isValid(rawId))
        count <- dbo.expand[Int]("count")
      } yield ItemCount(new ObjectId(rawId), count)
    }

    def toEmptyItemCount(id: ObjectId) = ItemCount(id, 0)

    val in: MongoDBObject = ("collectionId" $in collectionIds.map(_.toString))
    val matchQuery = MongoDBObject("$match" -> in)
    val group = MongoDBObject("$group" -> MongoDBObject("_id" -> "$collectionId", "count" -> MongoDBObject("$sum" -> 1)))
    val output = currentCollection.aggregate(Seq(matchQuery, group))
    val foundCounts = output.results.toSeq.flatMap(toItemCount)
    logger.trace(s"function=countItemsInCollections, foundCounts=$foundCounts")
    val emptyCounts = collectionIds.filterNot(id => foundCounts.exists(_.collectionId == id)).map(toEmptyItemCount)
    logger.trace(s"function=countItemsInCollections, emptyCounts=$emptyCounts")
    val out = foundCounts ++ emptyCounts
    logger.trace(s"function=countItemsInCollections, out=$out")

    require(collectionIds.length == out.length, "Missing item counts")
    out.sortBy(_.collectionId)
  }

  override def collectionIdForItem(itemId: VersionedId[ObjectId]): Option[ObjectId] = {
    dao.findDbo(itemId.copy(version = None), MongoDBObject(Keys.collectionId -> 1)).flatMap {
      dbo =>
        try {
          val idString = dbo.get(Keys.collectionId).asInstanceOf[String]
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

  override def findItemStandards(itemId: VersionedId[ObjectId]): Option[ItemStandards] = {
    val fields = MongoDBObject("taskInfo.title" -> 1, Keys.standards -> 1)
    for {
      dbo <- dao.findDbo(itemId, fields)
      _ <- Some(logger.debug(s"function=findItemStandards, dbo=$dbo"))
      title <- dbo.expand[String]("taskInfo.title")
      _ <- Some(logger.trace(s"function=findItemStandards, title=$title"))
      standards <- dbo.expand[Seq[String]](Keys.standards)
      _ <- Some(logger.trace(s"function=findItemStandards, standards=$standards"))
    } yield ItemStandards(title, standards, itemId)
  }

}
