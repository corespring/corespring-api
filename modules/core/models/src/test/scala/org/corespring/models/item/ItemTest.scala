package org.corespring.models.item

import org.bson.types.ObjectId
import org.corespring.models.item.Item.QtiResource.QtiXml
import org.corespring.models.item.resource.{ VirtualFile, Resource }
import org.specs2.mutable.Specification

class ItemTest extends Specification {

  "createdByApiVersion" should {

    def item(qti: Boolean, playerDefinition: Boolean): Item = {

      val data = if (qti) {
        Some(Resource(name = "data", files = Seq(VirtualFile(isMain = true, name = QtiXml, content = "<xml/>", contentType = "text/xml"))))
      } else None

      val pd = if (playerDefinition) {
        Some(PlayerDefinition.empty)
      } else None

      Item(collectionId = ObjectId.get.toString, data = data, playerDefinition = pd)
    }

    def assertItem(qti: Boolean, playerDefinition: Boolean, version: Int) = {
      s"return $version for qti: $qti, playerDefinition:$playerDefinition" in {
        item(qti, playerDefinition).createdByApiVersion === version
      }
    }

    assertItem(qti = true, playerDefinition = true, version = 1)
    assertItem(qti = false, playerDefinition = true, version = 2)
    assertItem(qti = true, playerDefinition = false, version = 1)
    assertItem(qti = false, playerDefinition = false, version = -1)
  }

  "cloning" should {

    val collectionId = ObjectId.get.toString

    "prepend [copy] to title" in {
      val item = Item(collectionId = collectionId, taskInfo = Some(TaskInfo(title = Some("something"))))
      item.cloneItem().taskInfo.get.title.get === "[copy] " + item.taskInfo.get.title.get
    }

    "prepend [copy] to empty taskinfo" in {
      val item = Item(collectionId = collectionId)
      item.cloneItem().taskInfo.get.title.get === "[copy]"
    }

    "prepend [copy] to empty title" in {
      val item = Item(collectionId = collectionId, taskInfo = Some(TaskInfo()))
      item.cloneItem().taskInfo.get.title.get === "[copy]"
    }

    "sets a new collectionId" in {
      val newCollectionId = ObjectId.get.toString
      val item = Item(collectionId = collectionId, taskInfo = Some(TaskInfo()))
      item.cloneItem(newCollectionId).collectionId === newCollectionId
    }

    "sets clonedFrom to None for new Item" in {
      val item = Item(collectionId = collectionId, taskInfo = Some(TaskInfo()))
      item.clonedFromId === None
    }

    "sets clonedFrom to original item" in {
      val item = Item(collectionId = collectionId, taskInfo = Some(TaskInfo()))
      item.cloneItem().clonedFromId === Some(item.id)
    }
  }

}

