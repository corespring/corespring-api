package org.corespring.services.salat.item

import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.item.resource.{ CloneFileResult, Resource, StoredFile }
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.ContentCollectionService
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito

import scalaz.{ Success, Failure, Validation }

class ItemServiceTest extends ServicesSalatIntegrationTest with Mockito {

  lazy val itemService = services.item

  "cloning" should {

    "create a new item in the db" in {
      val item = Item(collectionId = "1234567")
      val clonedItem = itemService.clone(item)
      item.collectionId === clonedItem.get.collectionId
      val update = item.copy(collectionId = "0987654321")
      itemService.save(update)
      val dbItem = itemService.findOneById(clonedItem.get.id)
      dbItem.isDefined === true
      clonedItem.map(_.collectionId) === dbItem.map(_.collectionId)
    }
  }

  "save" should {

    def mockAssets(succeed: Boolean) = {
      val m = mock[ItemAssetService]

      m.cloneStoredFiles(any[Item], any[Item]).answers { (args) =>
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

    def itemServiceWithMockFiles(succeed: Boolean) = new ItemService(
      itemService.asInstanceOf[ItemService].dao,
      mockAssets(succeed),
      mock[ContentCollectionService],
      services.context,
      services.archiveConfig) {
    }

    def assertSaveWithStoredFile(name: String, shouldSucceed: Boolean): MatchResult[Any] = {
      val service = itemServiceWithMockFiles(shouldSucceed)
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

      println("expecting version: " + expectedVersion)

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

  "session count" should {
    "count" in pending("session count to be moved")
  }
}
