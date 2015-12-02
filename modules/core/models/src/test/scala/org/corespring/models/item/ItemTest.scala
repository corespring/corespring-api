package org.corespring.models.item

import org.specs2.mutable.Specification

class ItemTest extends Specification {

  "cloning" should {

    "prepend [copy] to title" in {
      val item = Item(collectionId = "1234567", taskInfo = Some(TaskInfo(title = Some("something"))))
      item.cloneItem.taskInfo.get.title.get === "[copy] " + item.taskInfo.get.title.get
    }

    "prepend [copy] to empty taskinfo" in {
      val item = Item(collectionId = "1234567")
      item.cloneItem.taskInfo.get.title.get === "[copy]"
    }

    "prepend [copy] to empty title" in {
      val item = Item(collectionId = "1234567", taskInfo = Some(TaskInfo()))
      item.cloneItem.taskInfo.get.title.get === "[copy]"
    }
  }

}

