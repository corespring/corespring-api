package org.corespring.services.item

import com.mongodb.casbah.Imports._
import org.corespring.errors.PlatformServiceError
import org.corespring.models.item.{ PlayerDefinition, ItemStandards, Item }
import org.corespring.models.item.resource.StoredFile
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.xml.Elem
import scalaz.Validation

trait ItemServiceClient {
  def itemService: ItemService
}

case class ItemCount(collectionId: ObjectId, count: Long)

trait PlayerDefinitionService {
  def findMultiplePlayerDefinitions(orgId: ObjectId, ids: VersionedId[ObjectId]*): Future[Seq[(VersionedId[ObjectId], Validation[PlatformServiceError, PlayerDefinition])]]
}

trait ItemService extends BaseContentService[Item, VersionedId[ObjectId]]
  with PlayerDefinitionService {

  def addFileToPlayerDefinition(item: Item, file: StoredFile): Validation[String, Boolean]

  def addFileToPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean]

  def clone(item: Item): Validation[String, Item]

  /**
   * Note: it would be better to just have clone, but that method is used in the [[BaseContentService]],
   * so hopefully we can remove that and the conflate the methods
   * @param item
   * @param targetCollectionId - clone the item to this collection if specified else use the same collection as the item
   * @return
   */
  def cloneToCollection(item: Item, targetCollectionId: ObjectId): Validation[String, Item]

  def collectionIdForItem(itemId: VersionedId[ObjectId]): Option[ObjectId]

  def contributorsForOrg(orgId: ObjectId): Seq[String]

  def countItemsInCollections(collectionId: ObjectId*): Future[Seq[ItemCount]]

  def currentVersion(id: VersionedId[ObjectId]): Long

  @deprecated("if requesting a part of the item, add a service api for that, like findItemStandards", "core-refactor")
  def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject]

  def findItemStandards(itemId: VersionedId[ObjectId]): Option[ItemStandards]

  def findMultipleById(ids: ObjectId*): Stream[Item]

  def findOneById(id: VersionedId[ObjectId]): Option[Item]

  def getOrCreateUnpublishedVersion(id: VersionedId[ObjectId]): Option[Item]

  def insert(i: Item): Option[VersionedId[ObjectId]]

  def moveItemToArchive(id: VersionedId[ObjectId]): Option[String]

  def publish(id: VersionedId[ObjectId]): Boolean

  /** Completely remove the item from the system. */
  def purge(id: VersionedId[ObjectId]): Validation[PlatformServiceError, VersionedId[ObjectId]]

  def removeFileFromPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean]

  def save(item: Item, createNewVersion: Boolean = false): Validation[PlatformServiceError, VersionedId[ObjectId]]

  def saveNewUnpublishedVersion(id: VersionedId[ObjectId]): Option[VersionedId[ObjectId]]

}

