package org.corespring.platform.core.services.item

import com.mongodb.{ BasicDBObject, DBObject }
import com.novus.salat.dao.SalatMongoCursor
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import scala.xml.Elem
import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatMongoCursor
import org.corespring.platform.core.models.error

trait ItemServiceClient {
  def itemService: ItemService
}

trait ItemService extends BaseItemService[VersionedId[ObjectId]]

trait BaseItemService[ID] {
  def cloneItem(item: Item): Option[Item]

  def findFieldsById(id: ID, fields: DBObject = new BasicDBObject()): Option[DBObject]

  def currentVersion(id: ID): Option[Int]

  def countItems(query: DBObject, fields: Option[String] = None): Int

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[Item]

  def findOneById(id: ID): Option[Item]

  def findOne(query: DBObject): Option[Item]

  def save(i: Item, createNewVersion: Boolean = false)

  /** Save using a dbo - allows finer grained updates using $set */
  def saveUsingDbo(id: ID, dbo: DBObject, createNewVersion: Boolean = false)

  def insert(i: Item): Option[ID]

  def findMultiple(ids: Seq[ID], keys: DBObject = new BasicDBObject()): Seq[Item]

  def getQtiXml(id: ID): Option[Elem]

  def sessionCount(item: Item): Long

  def createDefaultCollectionsQuery[A](collections: Seq[ObjectId], orgId: ObjectId): MongoDBObject

  def parseCollectionIds[A](organizationId: ObjectId)(value: AnyRef): Either[error.InternalError, AnyRef]
}
