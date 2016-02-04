package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatDAOUpdateError
import grizzled.slf4j.Logger
import org.corespring.errors._
import org.corespring.errors.collection.OrgNotAuthorized
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService

import scalaz.{ Validation, Failure, Success }

class ShareItemWithCollectionsService(
  dao: VersioningDao[Item, VersionedId[ObjectId]],
  itemService: => ItemService,
  orgCollectionService: => org.corespring.services.OrgCollectionService) extends org.corespring.services.ShareItemWithCollectionsService {

  private val logger = Logger(classOf[ShareItemWithCollectionsService])

  import org.corespring.models.item.Item.Keys

  private def addCollectionIdToSharedCollections(itemIds: Seq[VersionedId[ObjectId]], collectionId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
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

  private def removeCollectionIdsFromShared(itemIds: Seq[VersionedId[ObjectId]], collectionIds: Seq[ObjectId]): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
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

  /**
   * Delete collection reference from shared collections (defined in items)
   * @return
   */
  override def unShareAllItemsFromCollection(collectionId: ObjectId): Validation[PlatformServiceError, Unit] = {
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
  /**
   * Unshare the specified items from the specified collections
   */
  override def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: ObjectId*): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {

    lazy val nonWritableCollections = collIds.map { c =>
      (c -> orgCollectionService.isAuthorized(orgId, c, Permission.Write))
    }.filter(_._2 == false).map(_._1)

    for {
      _ <- if (nonWritableCollections.nonEmpty) Failure(PlatformServiceError(s"Can't write to the following collections: $nonWritableCollections")) else Success()
      removed <- removeCollectionIdsFromShared(items, collIds)
    } yield removed
  }

  /**
   * Share items to the collection specified.
   * - must ensure that the context org has write access to the collection
   * - must ensure that the context org has read access to the items being added
   * TODO: Do we check perms here? or keep it outside of this scope?
   * We'll have to filter the individual item ids anyway
   */
  override def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {

    lazy val canRead = {
      val objectIds = items.map(i => i.id)
      // get a list of any items that were not authorized to be added
      itemService.findMultipleById(objectIds: _*).filterNot(item => {
        // get the collections to test auth on (owner collection for item, and shared-in collections)
        val collectionsToAuth = new ObjectId(item.collectionId) +: item.sharedInCollections
        // does org have read access to any of these collections
        val collectionsAuthorized = collectionsToAuth.
          filter(collectionId => orgCollectionService.isAuthorized(orgId, collectionId, Permission.Read))
        collectionsAuthorized.nonEmpty
      }) match {
        case Stream.Empty => Success()
        case notAuthorizedItems => {
          logger.error(s"[allowedToReadItems] unable to read items: ${notAuthorizedItems.map(_.id)}")
          Failure(ItemAuthorizationError(orgId, Permission.Read, notAuthorizedItems.map(_.id): _*))
        }
      }
    }

    def saveUnsavedItems = {
      addCollectionIdToSharedCollections(items, collId)
    }

    lazy val canWrite = if (orgCollectionService.isAuthorized(orgId, collId, Permission.Write)) {
      Success()
    } else {
      import org.corespring.errors.collection._
      Failure(CantWriteToCollection(orgId, None, collId))
    }

    for {
      _ <- canWrite
      _ <- canRead
      sharedItems <- saveUnsavedItems
    } yield sharedItems
  }

  override def isItemSharedWith(itemId: VersionedId[ObjectId], collId: ObjectId): Boolean = {
    itemService.findOneById(itemId).map(_.sharedInCollections.contains(collId)).getOrElse(false)
  }
}
