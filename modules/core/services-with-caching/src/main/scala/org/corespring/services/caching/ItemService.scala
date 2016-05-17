package org.corespring.services.caching

import com.mongodb.casbah.Imports._
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.item.resource.StoredFile
import org.corespring.models.item.{ Item, ItemStandards, PlayerDefinition }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemCount
import org.corespring.{ services => interface }

import scala.concurrent.Future
import scalaz.Validation

class ItemService(underlying: interface.item.ItemService) extends interface.item.ItemService {
  override def addFileToPlayerDefinition(item: Item, file: StoredFile): Validation[String, Boolean] = {
    underlying.addFileToPlayerDefinition(item, file)
  }

  override def addFileToPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean] = {
    underlying.addFileToPlayerDefinition(itemId, file)
  }

  override def clone(item: Item): Validation[String, Item] = {
    underlying.clone(item)
  }

  override def cloneToCollection(item: Item, targetCollectionId: ObjectId): Validation[String, Item] = {
    underlying.cloneToCollection(item, targetCollectionId)
  }
  override def collectionIdForItem(itemId: VersionedId[ObjectId]): Option[ObjectId] = {
    underlying.collectionIdForItem(itemId)
  }
  override def contributorsForOrg(orgId: ObjectId): Seq[String] = {
    underlying.contributorsForOrg(orgId)
  }
  override def countItemsInCollections(collectionId: ObjectId*): Future[Seq[ItemCount]] = {
    underlying.countItemsInCollections(collectionId: _*)
  }
  override def currentVersion(id: VersionedId[ObjectId]): Long = {
    underlying.currentVersion(id)
  }

  @deprecated("if requesting a part of the item, add a service api for that, like findItemStandards", "core-refactor")
  override def findFieldsById(id: VersionedId[ObjectId], fields: DBObject): Option[DBObject] = {
    underlying.findFieldsById(id, fields)
  }
  override def findItemStandards(itemId: VersionedId[ObjectId]): Option[ItemStandards] = {
    underlying.findItemStandards(itemId)
  }
  override def findMultipleById(ids: ObjectId*): Stream[Item] = {
    underlying.findMultipleById(ids: _*)
  }
  override def findMultiplePlayerDefinitions(orgId: ObjectId, ids: VersionedId[ObjectId]*): Future[Seq[(VersionedId[ObjectId], Validation[PlatformServiceError, PlayerDefinition])]] = {
    underlying.findMultiplePlayerDefinitions(orgId, ids: _*)
  }
  override def findOneById(id: VersionedId[ObjectId]): Option[Item] = {
    underlying.findOneById(id)
  }
  override def getOrCreateUnpublishedVersion(id: VersionedId[ObjectId]): Option[Item] = {
    underlying.getOrCreateUnpublishedVersion(id)
  }
  override def insert(i: Item): Option[VersionedId[ObjectId]] = {
    underlying.insert(i)
  }
  override def moveItemToArchive(id: VersionedId[ObjectId]): Option[String] = {
    underlying.moveItemToArchive(id)
  }
  override def publish(id: VersionedId[ObjectId]): Boolean = {
    underlying.publish(id)
  }

  /** Completely remove the item from the system. */
  override def purge(id: VersionedId[ObjectId]): Validation[PlatformServiceError, VersionedId[ObjectId]] = {
    underlying.purge(id)
  }
  override def removeFileFromPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean] = {
    underlying.removeFileFromPlayerDefinition(itemId, file)
  }
  override def save(item: Item, createNewVersion: Boolean): Validation[PlatformServiceError, VersionedId[ObjectId]] = underlying
  override def saveNewUnpublishedVersion(id: VersionedId[ObjectId]): Option[VersionedId[ObjectId]] = ???
  override def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Validation[PlatformServiceError, Unit] = ???

  override def isAuthorizedBatch(orgId: ObjectId, idAndPermissions: (VersionedId[ObjectId], Permission)*): Future[Seq[(VersionedId[ObjectId], Boolean)]] = ???
}