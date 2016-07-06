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
import org.corespring.models.item.{ Item, ItemStandards, PlayerDefinition }
import org.corespring.mongo.IdConverters
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.CollectionIdPermission
import org.corespring.services.item.ItemCount
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.corespring.{ services => interface }
import org.joda.time.DateTime
import org.corespring.macros.DescribeMacro.{ describe => ds }

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

    logger.debug(ds(item.id, result))

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
    logger.trace(ds(id))
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
    logger.trace(ds(itemId, result))
    Validation.fromEither(result).map(id => true)
  }

  override def addFileToPlayerDefinition(item: Item, file: StoredFile): Validation[String, Boolean] = addFileToPlayerDefinition(item.id, file)

  override def removeFileFromPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean] = {
    val dbo = com.novus.salat.grater[StoredFile].asDBObject(file)
    val update = MongoDBObject("$pull" -> MongoDBObject("playerDefinition.files" -> dbo))
    val result = dao.update(itemId, update, false)
    logger.trace(ds(itemId, result))
    Validation.fromEither(result).map(id => true)
  }

  import org.corespring.services.salat.ValidationUtils._

  // three things occur here:
  // 1. save the new item,
  // 2. copy the old item's s3 files,
  // 3. update the old item's stored files with the new s3 locations
  // TODO if any of these three things fail, the database and s3 revert back to previous state
  override def save(item: Item, createNewVersion: Boolean = false): Validation[PlatformServiceError, VersionedId[ObjectId]] = {

    logger.trace(ds(createNewVersion, item))

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
      logger.trace(ds(result))

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

  private def toVid(dbo: DBObject): VersionedId[ObjectId] = com.novus.salat.grater[VersionedId[ObjectId]].asObject(dbo)

  type CollToVidMap = Map[CollectionIdPermission, Seq[VersionedId[ObjectId]]]

  override def isAuthorizedBatch(orgId: ObjectId, idAndPermissions: VidPerm*): Future[Seq[(VidPerm, Boolean)]] = {

    idAndPermissions match {
      case Nil => Future.successful(Nil)
      case _ => {

        lazy val futureItemAndCollectionIds = Future {
          val fields = MongoDBObject(Keys.collectionId -> 1)
          dao.findDbos(idAndPermissions.map(_._1), fields).foldRight(Map.empty[CollectionIdPermission, Seq[VersionedId[ObjectId]]]) { (dbo, acc) =>

            def getIdPermissionMap(vid: VersionedId[ObjectId], p: Permission, map: CollToVidMap) = {
              val collectionIdString = dbo.get(Keys.collectionId).asInstanceOf[String]
              val oid = new ObjectId(collectionIdString)
              val coll: Seq[VersionedId[ObjectId]] = map.get(CollectionIdPermission(oid, p)).getOrElse(Seq.empty)
              Map(CollectionIdPermission(oid, p) -> (coll :+ vid))
            }

            val vid = toVid(dbo.get("_id").asInstanceOf[DBObject])
            acc ++ idAndPermissions.filter(_._1 == vid).foldRight(acc) { (tuple, m) =>
              val (_, p) = tuple
              m ++ getIdPermissionMap(vid, p, m)
            }
          }
        }

        logger.trace(ds(idAndPermissions))

        for {
          collectionIdToVidMap <- futureItemAndCollectionIds
          _ <- Future.successful(logger.trace(ds(collectionIdToVidMap)))
          collectionResults <- orgCollectionService.isAuthorizedBatch(orgId, collectionIdToVidMap.keys.toSeq.distinct: _*)
        } yield {

          logger.trace(ds(collectionResults, collectionIdToVidMap))
          idAndPermissions.map {
            case (vid, p) =>
              val collectionIdToVid = collectionIdToVidMap.find {
                case (idp, itemIds) => {
                  itemIds.contains(vid) && idp.permission == p
                }
              }

              collectionIdToVid.map {
                case (collectionIdPermission, itemIds) => {
                  logger.trace(ds(collectionIdPermission, itemIds))
                  val authorized = collectionResults.find {
                    case ((idp, authorized)) if (idp == collectionIdPermission) => authorized
                    case _ => false
                  }.map(_._2).getOrElse(false)

                  (vid -> p) -> authorized
                }
              }.getOrElse((vid -> p) -> false)
          }
        }
      }
    }
  }

  override def contributorsForOrg(orgId: ObjectId): Seq[String] = {

    val readableCollectionIds = orgCollectionService
      .getCollections(orgId, Permission.Read)
      .fold(_ => Seq.empty, c => c)
      .map(_.id)
      .filterNot(_ == archiveConfig.contentCollectionId)
      .map(_.toString)

    logger.trace(ds(readableCollectionIds))

    val filter = baseQuery ++ MongoDBObject(
      Keys.collectionId -> MongoDBObject("$in" -> readableCollectionIds))
    //TODO: RF - include versioned content?

    logger.trace(ds(filter))
    dao.distinct("contributorDetails.contributor", filter).map(_.toString)
  }

  //TODO - would db("content").group be quicker?
  override def countItemsInCollections(collectionIds: ObjectId*): Future[Seq[ItemCount]] = Future {

    logger.debug(ds(collectionIds))

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
    logger.trace(ds(foundCounts))
    val emptyCounts = collectionIds.filterNot(id => foundCounts.exists(_.collectionId == id)).map(toEmptyItemCount)
    logger.trace(ds(emptyCounts))
    val out = foundCounts ++ emptyCounts
    logger.trace(ds(out))

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
      _ <- Some(logger.debug(ds(dbo)))
      title <- dbo.expand[String]("taskInfo.title")
      _ <- Some(logger.trace(ds(title)))
      standards <- dbo.expand[Seq[String]](Keys.standards)
      _ <- Some(logger.trace(ds(standards)))
    } yield ItemStandards(title, standards, itemId)
  }

  override def findMultiplePlayerDefinitions(orgId: ObjectId, ids: VersionedId[ObjectId]*): Future[Seq[(VersionedId[ObjectId], Validation[PlatformServiceError, PlayerDefinition])]] = {

    ids match {
      case Nil => Future.successful(Nil)
      case _ => {

        logger.debug(ds(orgId, ids))
        isAuthorizedBatch(orgId, ids.map(id => (id, Permission.Read)): _*).map { ids =>

          logger.trace(ds(ids))

          val (valid, invalid) = ids.partition(_._2)

          val validIds = valid.map(t => t._1._1)
          val invalidResults = invalid.map(_._1._1 -> Failure(PlatformServiceError("Not authorized to access")))

          logger.trace(ds(orgId, ids, validIds))

          import scalaz.Scalaz._

          val validResults: Seq[(VersionedId[ObjectId], Validation[PlatformServiceError, PlayerDefinition])] = if (validIds.length == 0) {
            logger.warn("function=findMultiplePlayerDefinitions - No valid ids found")
            Nil
          } else {
            dao.findDbos(validIds, MongoDBObject(Keys.playerDefinition -> 1))
              .map(dbo => {
                logger.trace(ds(orgId, dbo))
                val idDbo = dbo.get("_id").asInstanceOf[DBObject]
                val vid = toVid(idDbo)
                val definition = dbo.get("playerDefinition").asInstanceOf[DBObject]
                if (definition == null) {
                  (vid -> Failure(PlatformServiceError("No player definition")))
                } else {
                  val maybeDef = try {
                    Some(com.novus.salat.grater[PlayerDefinition].asObject(definition))
                  } catch {
                    case t: Throwable => {
                      logger.error(t)
                      t.printStackTrace()
                      None
                    }
                  }
                  (vid -> maybeDef.toSuccess(PlatformServiceError("Unable to convert playerDefinition")))
                }
              })
          }

          invalidResults ++ validResults
        }
      }
    }
  }

}
