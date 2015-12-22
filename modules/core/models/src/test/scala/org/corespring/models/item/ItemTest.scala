package org.corespring.models.item

import org.bson.types.ObjectId
import org.specs2.mutable.Specification

class ItemTest extends Specification {

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

