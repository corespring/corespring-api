package org.corespring.platform.core.files

import org.bson.types.ObjectId
import org.corespring.assets.CorespringS3Service
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ StoredFile, Resource }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.utils.mocks.MockS3Service
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scalaz.{ Failure, Success }

class ItemFilesTest extends Specification with Mockito {

  class itemFilesScope extends ItemFiles with Scope {
    val mockS3 = mock[CorespringS3Service]
    def s3service: CorespringS3Service = mockS3

    def bucket: String = "some-bucket"
  }

  "item files" should {

    "clone" in new itemFilesScope {

      val oldId = VersionedId(ObjectId.get, Some(0))
      val newId = VersionedId(ObjectId.get, Some(1))

      val filename = "img.png"
      val resourceName = "my-resource"

      val file = StoredFile(name = filename, contentType = "image/png")
      val resource = Resource(name = resourceName, files = Seq(file))

      file.storageKey = StoredFile.storageKey(oldId.id, oldId.version.get, resource, file.name)

      val item: Item = Item(id = newId, data = Some(resource))
      val cloned = item.cloneItem
      cloneStoredFiles(item, cloned) match {
        case Success(updatedItem) => {
          val file: StoredFile = updatedItem.data.get.files(0).asInstanceOf[StoredFile]
          file.storageKey === StoredFile.storageKey(cloned.id.id, cloned.id.version.get, resource, file.name)
        }
        case _ => failure("error")
      }
    }

    "if the storageKey is empty, then it is created" in new itemFilesScope {
      val file = StoredFile(name = "test.png", contentType = "image/png")
      val resource = Resource(name = "data", files = Seq(file))
      val item = Item(data = Some(resource))
      val cloned = item.cloneItem
      cloneStoredFiles(item, cloned)
      val from = StoredFile.storageKey(item.id.id, 0, "data", "test.png")
      val to = StoredFile.storageKey(cloned.id.id, 0, "data", "test.png")
      there was one(mockS3).copyFile(bucket, from, to)
    }

    "if clone fails for a file then the list of clone file results are returned" in new itemFilesScope {

      mockS3.copyFile(any[String], any[String], any[String]) throws (new RuntimeException("!!"))

      val oldId = VersionedId(ObjectId.get, Some(0))
      val newId = VersionedId(ObjectId.get, Some(1))

      val filename = "img.png"
      val resourceName = "my-resource"

      val goodFile = StoredFile(name = filename, contentType = "image/png")
      val badFile = StoredFile(name = "bad.png", contentType = "image/png")

      val resource = Resource(name = resourceName, files = Seq(goodFile, badFile))

      goodFile.storageKey = StoredFile.storageKey(oldId.id, oldId.version.get, resource, goodFile.name)
      badFile.storageKey = StoredFile.storageKey(oldId.id, oldId.version.get, resource, badFile.name)

      val item: Item = Item(id = newId, data = Some(resource))

      cloneStoredFiles(item, item.cloneItem) match {
        case Success(updatedItem) => failure("error")
        case Failure(cloneFileResults) => {
          cloneFileResults.length === 2
          cloneFileResults(0).file.name === "img.png"
          cloneFileResults(1).file.name === "bad.png"
        }
      }
    }
  }

}
