package org.corespring.test.helpers.models

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import scala.Some
import org.corespring.platform.core.models.item.resource.{Resource, VirtualFile}

object ItemHelper {

  // It would be great if this could return the item id
  def create(collectionId: ObjectId): VersionedId[ObjectId] = {

    val qti = VirtualFile("qti.xml", "text/xml", true, "<assessmentItem><itemBody></itemBody></assessmentItem>")
    val data : Resource = Resource( name = "data", files = Seq(qti))
    val item = Item(collectionId = collectionId.toString, data = Some(data))

    ItemServiceWired.insert(item) match {
      case Some(versionedId) => versionedId
      case _ => throw new Exception("Error creating item")
    }
  }

  def count(collectionIds: Option[Seq[ObjectId]] = None): Int = {
    collectionIds match {
      case Some(ids) =>
        ItemServiceWired.countItems(MongoDBObject("collectionId" -> MongoDBObject("$in" -> ids.map(_.toString))))
      case _ => ItemServiceWired.countItems(MongoDBObject())
    }
  }

  /**
   * Provides a count of all items in public collections
   */
  def publicCount: Int = count(Some(CollectionHelper.public))

  def delete(itemId: VersionedId[ObjectId]) = ItemServiceWired.deleteUsingDao(itemId)

}
