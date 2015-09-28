package org.corespring.platform.core.services.item

import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ StoredFileDataStream, BaseFile, Resource, StoredFile }
import org.corespring.platform.data.mongo.models.VersionedId
import com.mongodb.casbah.Imports._
import org.corespring.platform.core.services.BaseContentService
import scala.concurrent.Future
import scala.xml.Elem
import com.mongodb.casbah.commons.MongoDBObject

import scalaz.Validation

trait ItemServiceClient {
  def itemService: ItemService
}

trait ItemService extends BaseContentService[Item, VersionedId[ObjectId]] {

  def getQtiXml(id: VersionedId[ObjectId]): Option[Elem]

  def sessionCount(item: Item): Long

  def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean = false)

  def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject]

  def moveItemToArchive(id: VersionedId[ObjectId])

  def publish(id: VersionedId[ObjectId]): Boolean

  def saveNewUnpublishedVersion(id: VersionedId[ObjectId]): Option[VersionedId[ObjectId]]

  /**
   * TODO: This should be removed, as it is a leaky abstraction.
   */
  def collection: MongoCollection

  def currentVersion(id: VersionedId[ObjectId]): Long

  def isPublished(id: VersionedId[ObjectId]): Boolean

  def addFileToPlayerDefinition(i: Item, f: StoredFile): Validation[String, Boolean]

}

trait SupportingMaterialsService[ID] {
  def create(id: ID, resource: Resource, bytes: => Array[Byte]): Validation[String, Resource]
  def delete(id: ID, materialName: String): Validation[String, Seq[Resource]]
  def removeFile(id: ID, materialName: String, filename: String): Validation[String, Resource]
  def addFile(id: ID, materialName: String, file: BaseFile, bytes: => Array[Byte]): Validation[String, Resource]
  def getFile(id: ID, materialName: String, file: String, etag: Option[String] = None): Validation[String, StoredFileDataStream]
  def updateFileContent(id: ID, materialName: String, file: String, content: String): Validation[String, Resource]
}

trait ItemPublishingService {

  def getOrCreateUnpublishedVersion(id: VersionedId[ObjectId]): Option[Item]
}
