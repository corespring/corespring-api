package org.corespring.services.item

import com.mongodb.casbah.Imports._
import org.corespring.models.item.{ ItemStandards, Item }
import org.corespring.models.item.resource.StoredFile
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError

import scala.xml.Elem
import scalaz.Validation

trait ItemServiceClient {
  def itemService: ItemService
}

trait ItemService extends BaseContentService[Item, VersionedId[ObjectId]] {

  def findItemStandards(itemId: VersionedId[ObjectId]): Option[ItemStandards]

  def collectionIdForItem(itemId: VersionedId[ObjectId]): Option[ObjectId]

  def countItemsInCollection(collectionId: ObjectId): Long

  def contributorsForOrg(orgId: ObjectId): Seq[String]

  @deprecated("use SessionService.sessionCount(itemId)", "")
  def sessionCount(id: VersionedId[ObjectId]): Long

  def clone(item: Item): Option[Item]

  def moveItemToArchive(id: VersionedId[ObjectId]): Option[String]

  def asMetadataOnly(i: Item): DBObject

  def publish(id: VersionedId[ObjectId]): Boolean

  def isPublished(id: VersionedId[ObjectId]): Boolean

  def saveNewUnpublishedVersion(id: VersionedId[ObjectId]): Option[VersionedId[ObjectId]]

  def count(query: DBObject, fields: Option[String] = None): Long

  def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject]

  def currentVersion(id: VersionedId[ObjectId]): Long

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): Stream[Item]

  def findOneById(id: VersionedId[ObjectId]): Option[Item]

  def findMultipleById(ids: ObjectId*): Stream[Item]

  def findOne(query: DBObject): Option[Item]

  def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean = false): Boolean

  /** Completely remove the item from the system. */
  def purge(id: VersionedId[ObjectId]): Validation[PlatformServiceError, VersionedId[ObjectId]]

  def addFileToPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean]

  def addFileToPlayerDefinition(item: Item, file: StoredFile): Validation[String, Boolean]

  def save(item: Item, createNewVersion: Boolean = false): Validation[PlatformServiceError, VersionedId[ObjectId]]

  def insert(i: Item): Option[VersionedId[ObjectId]]

  def findMultiple(ids: Seq[VersionedId[ObjectId]], keys: DBObject): Seq[Item]

  def getQtiXml(id: VersionedId[ObjectId]): Option[Elem]

  def addCollectionIdToSharedCollections(itemIds: Seq[VersionedId[ObjectId]], collectionId: ObjectId): Validation[PlatformServiceError, Unit]

  def removeCollectionIdsFromShared(itemIds: Seq[VersionedId[ObjectId]], collId: Seq[ObjectId]): Validation[PlatformServiceError, Unit]

  /**
   * Delete collection reference from shared collections (defined in items)
   * @param collectionId
   * @return
   */
  def deleteFromSharedCollections(collectionId: ObjectId): Validation[PlatformServiceError, Unit]

  def getOrCreateUnpublishedVersion(id: VersionedId[ObjectId]): Option[Item]

}

