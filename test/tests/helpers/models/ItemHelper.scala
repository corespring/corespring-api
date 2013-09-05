package tests.helpers.models

import org.bson.types.ObjectId
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import com.mongodb.casbah.commons.MongoDBObject

object ItemHelper {

  // It would be great if this could return the item id
  def create(collectionId: ObjectId): VersionedId[ObjectId] = {
    ItemServiceImpl.insert(Item(collectionId = collectionId.toString())) match {
      case Some(versionedId) => versionedId
      case _ => throw new Exception("Error creating item")
    }
  }

  def count: Int = {
    ItemServiceImpl.countItems(MongoDBObject())
  }

  def delete(itemId: VersionedId[ObjectId]) = ItemServiceImpl.deleteUsingDao(itemId)

}
