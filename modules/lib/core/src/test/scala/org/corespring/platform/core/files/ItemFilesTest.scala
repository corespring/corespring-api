package org.corespring.platform.core.files

import org.bson.types.ObjectId
import org.corespring.assets.CorespringS3Service
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ StoredFile, Resource }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.utils.mocks.MockS3Service
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import scalaz.{ Failure, Success }

class ItemFilesTest extends Specification with Mockito {

  val itemFiles = new ItemFiles {
    def s3service: CorespringS3Service = mock[CorespringS3Service]

    def bucket: String = "some-bucket"
  }

  "item files" should {

    "clone" in {

      val oldId = VersionedId(ObjectId.get, Some(0))
      val newId = VersionedId(ObjectId.get, Some(1))

      val filename = "img.png"
      val resourceName = "my-resource"

      val file = StoredFile(name = filename, contentType = "image/png")
      val resource = Resource(name = resourceName, files = Seq(file))

      file.storageKey = StoredFile.storageKey(oldId, resource, file.name)

      val item: Item = Item(id = newId, data = Some(resource))

      itemFiles.cloneStoredFiles(item) match {
        case Success(updatedItem) => {
          val file: StoredFile = updatedItem.data.get.files(0).asInstanceOf[StoredFile]
          //true === true
          file.storageKey === StoredFile.storageKey(newId, resource, file.name)
          //success
        }
        case _ => failure("error")
      }
    }

    "if clone fails for a file then the list of clone file results are returned" in {

      val mockFiles = new ItemFiles {
        def s3service: CorespringS3Service = new MockS3Service
        def bucket: String = "blah"
      }

      val oldId = VersionedId(ObjectId.get, Some(0))
      val newId = VersionedId(ObjectId.get, Some(1))

      val filename = "img.png"
      val resourceName = "my-resource"

      val goodFile = StoredFile(name = filename, contentType = "image/png")
      val badFile = StoredFile(name = "bad.png", contentType = "image/png")

      val resource = Resource(name = resourceName, files = Seq(goodFile, badFile))

      goodFile.storageKey = StoredFile.storageKey(oldId, resource, goodFile.name)
      badFile.storageKey = StoredFile.storageKey(oldId, resource, badFile.name)

      val item: Item = Item(id = newId, data = Some(resource))

      mockFiles.cloneStoredFiles(item) match {
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
