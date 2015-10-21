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

class ItemServiceTest extends ServicesSalatIntegrationTest with Mockito {

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

  "save" should {

    def mockAssets(succeed: Boolean) = {
      val m = mock[ItemAssetService]

      m.cloneStoredFiles(any[Item], any[Item]).answers { (args, _) =>
        {
          val out: Validation[Seq[CloneFileResult], Item] = if (succeed) {
            val arr = args.asInstanceOf[Array[Any]]
            Success(arr(1).asInstanceOf[Item])
          } else {
            Failure(Seq.empty[CloneFileResult])
          }
          out
        }
      }
      m
    }

    def assertSaveWithStoredFile(name: String, shouldSucceed: Boolean): MatchResult[Any] = {
      val service = services.itemService
      val id = VersionedId(ObjectId.get)
      val file = StoredFile(name, "image/png", false, StoredFile.storageKey(id.id, 0, "data", name))
      val resource = Resource(name = "data", files = Seq(file))
      val item = Item(id = id, collectionId = "?", data = Some(resource), taskInfo = Some(TaskInfo(title = Some("original title"))))
      val latestId = service.insert(item)

      latestId.map { vid =>
        vid.version === Some(0)
      }.getOrElse(failure("insert failed"))

      val update = item.copy(id = latestId.get, taskInfo = Some(TaskInfo(title = Some("new title"))))

      service.save(update, true)

      val dbItem = service.findOneById(VersionedId(item.id.id))

      val expectedVersion = if (shouldSucceed) 1 else 0

      val out: MatchResult[Any] = dbItem
        .map(i => i.id === VersionedId(id.id, Some(expectedVersion))).get

      out
    }

    "revert the version if a failure occurred when cloning stored files" in {
      assertSaveWithStoredFile("bad.png", false)
    }

    "update the version if no failure occurred when cloning stored files" in {
      assertSaveWithStoredFile("good.png", true)
    }
  }

}
