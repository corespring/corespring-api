package org.corespring.platform.core.services.item

import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import com.mongodb.casbah.Imports._
import org.corespring.platform.core.services.BaseContentService
import scala.xml.Elem
import com.mongodb.casbah.commons.MongoDBObject

trait ItemServiceClient {
  def itemService: ItemService
}

trait ItemService extends BaseContentService[Item, VersionedId[ObjectId]] {

  def getQtiXml(id: VersionedId[ObjectId]): Option[Elem]

  def sessionCount(item: Item): Long

  def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean = false)

  def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject]

}
