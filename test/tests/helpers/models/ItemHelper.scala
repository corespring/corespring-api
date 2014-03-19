package tests.helpers.models

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.helpers.models.CollectionHelper
import scala.Some

object ItemHelper {

  // It would be great if this could return the item id
  def create(collectionId: ObjectId): VersionedId[ObjectId] = {
    ItemServiceWired.insert(Item(collectionId = Some(collectionId.toString()), published = true)) match {
      case Some(versionedId) => versionedId
      case _ => throw new Exception("Error creating item")
    }
  }

  def count(collectionIds: Option[Seq[ObjectId]] = None): Int = {
    collectionIds match {
      case Some(ids) =>
        ItemServiceWired.count(MongoDBObject("collectionId" -> MongoDBObject("$in" -> ids.map(_.toString))))
      case _ => ItemServiceWired.count(MongoDBObject())
    }
  }

  /**
   * Provides a count of all items in public collections
   */
  def publicCount: Int = count(Some(CollectionHelper.public))

  def delete(itemId: VersionedId[ObjectId]) = ItemServiceWired.deleteUsingDao(itemId)

}
