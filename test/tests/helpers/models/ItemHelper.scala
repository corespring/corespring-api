package tests.helpers.models

import org.bson.types.ObjectId
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.platform.core.models.item.Item.Keys._
import scala.Some
import com.mongodb.casbah.Imports._
import scala.Some

object ItemHelper {

  // It would be great if this could return the item id
  def create(collectionId: ObjectId): VersionedId[ObjectId] = {
    ItemServiceImpl.insert(Item(collectionId = collectionId.toString())) match {
      case Some(versionedId) => versionedId
      case _ => throw new Exception("Error creating item")
    }
  }

  def count(collectionIds: Option[Seq[ObjectId]] = None): Int = {
    collectionIds match {
      case Some(ids) =>
        ItemServiceImpl.countItems(MongoDBObject("collectionId" -> MongoDBObject("$in" -> ids.map(_.toString))))
      case _ => ItemServiceImpl.countItems(MongoDBObject())
    }
  }

  /**
   * Provides a count of all items in public collections
   */
  def publicCount: Int = count(Some(CollectionHelper.public))

  def delete(itemId: VersionedId[ObjectId]) = ItemServiceImpl.deleteUsingDao(itemId)

}
