package org.corespring.it.helpers

import bootstrap.Main
import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.util.JSON
import org.bson.types.ObjectId
import org.corespring.models.item.resource.{ Resource, VirtualFile }
import org.corespring.models.item.{ Item, TaskInfo }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.bootstrap.CollectionNames
import play.api.Logger
import play.api.libs.json.{ JsValue, Json }

object ItemHelper {

  val logger = Logger(ItemHelper.getClass)

  lazy val itemService = Main.itemService
  lazy val itemCollection = Main.db(CollectionNames.item)

  val qtiXmlTemplate = "<assessmentItem><itemBody>::version::</itemBody></assessmentItem>"
  def create(collectionId: ObjectId): VersionedId[ObjectId] = {
    val qti = VirtualFile("qti.xml", "text/xml", true, qtiXmlTemplate)
    val data: Resource = Resource(name = "data", files = Seq(qti))
    val item = Item(collectionId = collectionId.toString, data = Some(data), taskInfo = Some(TaskInfo(title = Some("Title"))))
    create(collectionId, item)
  }

  def update(itemId: VersionedId[ObjectId], json: JsValue): Unit = {
    val dbo: DBObject = com.mongodb.util.JSON.parse(Json.stringify(json)).asInstanceOf[DBObject]
    itemCollection.update(MongoDBObject("_id._id" -> itemId.id), dbo, upsert = false, multi = false)
  }

  def update(item: Item) = {
    itemService.save(item, false)
  }
  def get(id: VersionedId[ObjectId]): Option[Item] = {
    itemService.findOneById(id)
  }

  def create(collectionId: ObjectId, item: Item): VersionedId[ObjectId] = {
    itemService.insert(item.copy(collectionId = collectionId.toString)) match {
      case Some(versionedId) => {
        logger.trace(s"function=create, dbo=${Main.salatItemDao.currentCollection.findOne(MongoDBObject("_id._id" -> versionedId.id)).toString}")
        versionedId
      }
      case _ => throw new Exception("Error creating item")
    }
  }

  def delete(itemId: VersionedId[ObjectId]) = itemService.purge(itemId)

}