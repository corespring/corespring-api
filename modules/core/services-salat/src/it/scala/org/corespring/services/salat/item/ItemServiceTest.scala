package org.corespring.services.salat.item

import org.bson.types.ObjectId
import org.corespring.models.item.resource.{ CloneFileResult, Resource, StoredFile }
import org.corespring.models.item.{ Item, ItemStandards, TaskInfo }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.After

import scalaz.{ Failure, Success, Validation }

class ItemServiceTest extends ServicesSalatIntegrationTest {

  lazy val itemService = services.itemService

  "contributorsForOrg" should {
    "work" in pending
  }

  "findItemStandards" should {
    trait scope extends After {

      lazy val item = Item(
        collectionId = ObjectId.get.toString,
        taskInfo = Some(TaskInfo(title = Some("title"))),
        standards = Seq("S1", "S2"))
      lazy val itemId = services.itemService.insert(item).get

      override def after: Any = {
        services.itemService.purge(itemId)
      }
    }

    "return an item standard" in new scope {
      services.itemService.findItemStandards(itemId) must_== Some(ItemStandards("title", Seq("S1", "S2"), itemId))
    }
  }

  "cloning" should {

    "create a new item in the db" in {
      val item = Item(collectionId = "1234567")
      val clonedItem = itemService.clone(item)
      item.collectionId === clonedItem.get.collectionId
      val update = item.copy(collectionId = "0987654321")
      itemService.save(update)
      itemService.findOneById(clonedItem.get.id) must_== clonedItem
    }
  }
}
