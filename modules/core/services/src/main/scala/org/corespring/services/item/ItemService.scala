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

trait ItemService extends BaseContentService[Item, VersionedId[ObjectId]] {

  def addFileToPlayerDefinition(item: Item, file: StoredFile): Validation[String, Boolean]

  def addFileToPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Validation[String, Boolean]

  def clone(item: Item): Option[Item]

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

  def save(item: Item, createNewVersion: Boolean = false): Validation[PlatformServiceError, VersionedId[ObjectId]]

  def saveNewUnpublishedVersion(id: VersionedId[ObjectId]): Option[VersionedId[ObjectId]]

  @deprecated("once we add a new ItemUpdateService for updating parts of the item remove this", "core-refactor")
  def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean = false): Boolean
}

