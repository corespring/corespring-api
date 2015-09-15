package org.corespring.it.helpers.models

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.util.JSON
import org.bson.types.ObjectId
import org.corespring.models.item.resource.{ Resource, VirtualFile }
import org.corespring.models.item.{ Item, TaskInfo }
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{ JsValue, Json }

/**
 * TODO: This helper is problematic because it violates the Law of Demeter by asking the ItemService for its underlying
 * collection. This creates a leaky abstraction, and so this should be refactored to use the collection directly.
 */
object ItemHelper {
  val qtiXmlTemplate = "<assessmentItem><itemBody>::version::</itemBody></assessmentItem>"
  def create(collectionId: ObjectId): VersionedId[ObjectId] = {
    val qti = VirtualFile("qti.xml", "text/xml", true, qtiXmlTemplate)
    val data: Resource = Resource(name = "data", files = Seq(qti))
    val item = Item(collectionId = Some(collectionId.toString), data = Some(data), taskInfo = Some(TaskInfo(title = Some("Title"))))
    create(collectionId, item)
  }

  def update(itemId: VersionedId[ObjectId], json: JsValue): Unit = {
    val dbo: DBObject = com.mongodb.util.JSON.parse(Json.stringify(json)).asInstanceOf[DBObject]
    ItemServiceWired.collection.update(MongoDBObject("_id._id" -> itemId.id), dbo, upsert = false, multi = false)
  }

  def get(id: VersionedId[ObjectId]): Option[Item] = {
    ItemServiceWired.findOneById(id)
  }

  def publish(id: VersionedId[ObjectId]) = {
    ItemServiceWired.findOneById(id).map { item => ItemServiceWired.save(item.copy(published = true)) }
  }

  def create(collectionId: ObjectId, item: Item): VersionedId[ObjectId] = {
    ItemServiceWired.insert(item.copy(collectionId = Some(collectionId.toString))) match {
      case Some(versionedId) => versionedId
      case _ => throw new Exception("Error creating item")
    }
  }

  def publish(query: DBObject) = {
    println(s"[publish] query: ${JSON.serialize(query)}")
    ItemServiceWired.collection.update(query, MongoDBObject("$set" -> MongoDBObject("published" -> true)), multi = true)
    println(s"[publish] query count : ${ItemServiceWired.collection.count(query)}")
    val result = ItemServiceWired.collection.find(query, MongoDBObject("published" -> 1)).toSeq.map(JSON.serialize(_)).mkString(",")
    println(s"[publish] query result : $result")
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