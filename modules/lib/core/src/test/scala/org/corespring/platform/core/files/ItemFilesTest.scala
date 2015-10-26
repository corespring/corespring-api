package org.corespring.platform.core.files

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.bson.types.ObjectId
import org.corespring.assets.CorespringS3Service
import org.corespring.models.item.{ PlayerDefinition, Item }
import org.corespring.models.item.resource.{ StoredFile, Resource }
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json
import scalaz.{ Failure, Success }

class ItemFilesTest extends Specification with Mockito {

  class itemFilesScope extends ItemFiles with Scope {
    val mockS3 = mock[CorespringS3Service]
    def s3service: CorespringS3Service = mockS3

    def bucket: String = "some-bucket"

    val aPng = StoredFile(name = "a.png", contentType = "image/png", storageKey = "some-key-for-v1")
    val data = Resource(name = "data", files = Seq(aPng))
    val itemId = VersionedId(ObjectId.get, Some(0))
    val item: Item = Item(
      id = itemId,
      data = Some(data),
      playerDefinition = Some(
        PlayerDefinition(Seq(aPng), "", Json.obj(), "", None)))
    val cloned = item.cloneItem
  }

  "cloneStoredFiles" should {

    "return the updated item if cloning was successful" in new itemFilesScope {
      val result = cloneStoredFiles(item, cloned)

      result match {
        case Success(update) => {
          val file: StoredFile = update.data.get.files(0).asInstanceOf[StoredFile]
          file.storageKey === StoredFile.storageKey(cloned.id.id, cloned.id.version.get, "data", file.name)
        }
        case Failure(results) => failure("should have been successful")
      }
    }

    "if clone fails for a file then the list of clone file results are returned" in new itemFilesScope {
      mockS3.copyFile(any[String], any[String], any[String]) throws (new RuntimeException("!!"))
      cloneStoredFiles(item, cloned) match {
        case Success(updatedItem) => failure("should have failed")
        case Failure(cloneFileResults) => {
          cloneFileResults.length === 1
          cloneFileResults(0).file.name === aPng.name
          cloneFileResults(0).successful === false
        }
      }
    }

    "clone does not fail when a file is missing from the original item" in new itemFilesScope {
      val mockExp = new AmazonS3Exception("NoSuchKey")
      mockExp.setErrorCode("NoSuchKey")
      mockS3.copyFile(any[String], any[String], any[String]) throws mockExp
      cloneStoredFiles(item, cloned) must_== Success(cloned)
    }
  }

  "clonePlayerDefinitionFiles" should {

    "skip any files that have already been copied" in new itemFilesScope {
      val alreadyCopied = Seq(CloneFileSuccess(aPng, "some-key"))
      val result = clonePlayerDefinitionFiles(alreadyCopied, item, cloned)
      result === Seq.empty
    }

    "call s3.copyFile" in new itemFilesScope {
      val result = clonePlayerDefinitionFiles(Seq.empty, item, cloned)
      result.length === 1
      result(0).successful === true
      val fromKey = StoredFile.storageKey(item.id.id, item.id.version.get, "data", aPng.name)
      val toKey = StoredFile.storageKey(cloned.id.id, cloned.id.version.get, "data", aPng.name)
      there was one(mockS3).copyFile(bucket, fromKey, toKey)
    }

    "return a CloneFileFailure" in new itemFilesScope {
      val err = new RuntimeException("clone-error")
      mockS3.copyFile(any[String], any[String], any[String]) throws err
      val result = clonePlayerDefinitionFiles(Seq.empty, item, cloned)
      result.length === 1
      result(0) === CloneFileFailure(aPng, err)
      val fromKey = StoredFile.storageKey(item.id.id, item.id.version.get, "data", aPng.name)
      val toKey = StoredFile.storageKey(cloned.id.id, cloned.id.version.get, "data", aPng.name)
      there was one(mockS3).copyFile(bucket, fromKey, toKey)
    }
  }

  "cloneV1Files" should {

    "return the a single CloneFileSuccess" in new itemFilesScope {
      val result = cloneV1Files(item, cloned)
      println(result)
      result.length === 1
      val toKey = StoredFile.storageKey(cloned.id.id, cloned.id.version.get, "data", aPng.name)
      result(0) === CloneFileSuccess(aPng, toKey)
    }

    "if clone fails for a file then the list of clone file results are returned" in new itemFilesScope {
      val err = new RuntimeException("!!")
      mockS3.copyFile(any[String], any[String], any[String]) throws err
      val results = cloneV1Files(item, cloned)
      results.length === 1
      results(0) === CloneFileFailure(aPng, err)
    }
  }

}
