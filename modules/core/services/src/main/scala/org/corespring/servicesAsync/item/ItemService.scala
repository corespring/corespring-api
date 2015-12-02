package org.corespring.servicesAsync.item

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
  def itemService: Future[ItemService]
}

trait ItemService extends BaseContentService[Item, VersionedId[ObjectId]] {

  def findItemStandards(itemId: VersionedId[ObjectId]): Future[Option[ItemStandards]]

  def collectionIdForItem(itemId: VersionedId[ObjectId]): Future[Option[ObjectId]]

  def countItemsInCollection(collectionId: ObjectId): Future[Long]

  def contributorsForOrg(orgId: ObjectId): Future[Seq[String]]

  def clone(item: Item): Future[Option[Item]]

  def moveItemToArchive(id: VersionedId[ObjectId]): Future[Option[String]]

  def publish(id: VersionedId[ObjectId]): Future[Boolean]

  def saveNewUnpublishedVersion(id: VersionedId[ObjectId]): Future[Option[VersionedId[ObjectId]]]

  def currentVersion(id: VersionedId[ObjectId]): Future[Long]

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): Future[Stream[Item]]

  def findOneById(id: VersionedId[ObjectId]): Future[Option[Item]]

  def findMultipleById(ids: ObjectId*): Future[Stream[Item]]

  /** Completely remove the item from the system. */
  def purge(id: VersionedId[ObjectId]): Future[Validation[PlatformServiceError, VersionedId[ObjectId]]]

  def addFileToPlayerDefinition(itemId: VersionedId[ObjectId], file: StoredFile): Future[Validation[String, Boolean]]

  def addFileToPlayerDefinition(item: Item, file: StoredFile): Future[Validation[String, Boolean]]

  def save(item: Item, createNewVersion: Boolean = false): Future[Validation[PlatformServiceError, VersionedId[ObjectId]]]

  def insert(i: Item): Future[Option[VersionedId[ObjectId]]]

  def getOrCreateUnpublishedVersion(id: VersionedId[ObjectId]): Future[Option[Item]]
}

