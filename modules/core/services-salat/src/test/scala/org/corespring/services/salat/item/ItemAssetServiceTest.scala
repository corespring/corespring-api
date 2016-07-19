package org.corespring.services.salat.item

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.bson.types.ObjectId
import org.corespring.models.item._
import org.corespring.models.item.resource._
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json

import scalaz.{ Failure, Success }

class ItemAssetServiceTest extends Specification with Mockito {

  "ItemAssetService" should {

    trait S3 {
      def copyAsset(fromKey: String, toKey: String): Unit
      def deleteAsset(key: String): Unit
    }

    trait scope extends Scope {
      lazy val s3 = mock[S3]
      val service: ItemAssetService = new ItemAssetService(s3.copyAsset, s3.deleteAsset)
      def randomItemIdWithVersion = VersionedId(ObjectId.get, Some(0))
      def randomItemIdWithoutVersion = VersionedId(ObjectId.get, None)

      def mkItem(
        id: VersionedId[ObjectId] = randomItemIdWithVersion,
        files: Seq[BaseFile] = Seq.empty,
        supportingMaterials: Seq[Resource] = Seq.empty,
        data: Option[Resource] = None) = {
        val item = Item(
          id = id,
          collectionId = "collectionId",
          playerDefinition = Some(PlayerDefinition(files, "", Json.obj(), "", None, Json.obj())),
          supportingMaterials = supportingMaterials,
          data = data)
        item
      }

      def incVersion(toItem: Item) = toItem.copy(id = VersionedId(toItem.id.id, Some(toItem.id.version.get + 1)))

      def mkFile(name: String, storageKey: String) = StoredFile(name = name, contentType = "image/png", storageKey = storageKey)
      def mkData(file: BaseFile*) = Some(Resource(name = "test-resource", files = file))
      def mkSupportingMaterials(file: BaseFile*) = Seq(Resource(name = "test-resource", files = file))

      def itemIdToPath(item: Item) = s"${item.id.id}/${item.id.version.get}"
    }

    "cloneStoredFiles" should {

      "an AmazonS3Exception from copyObject returns a Failure" in new scope {
        val storedFileWithoutStorageKey = mkFile("test-file", "")
        val supportingMaterials = mkSupportingMaterials(storedFileWithoutStorageKey)
        val fromItem = mkItem(supportingMaterials = supportingMaterials)
        val toItem = incVersion(fromItem)
        s3.copyAsset(any[String], any[String]) throws {
          val e = new AmazonS3Exception("test-error")
          e.setStatusCode(500)
          e
        }
        service.cloneStoredFiles(fromItem, toItem).isFailure must_== true
      }

      "an AmazonS3Exception from copyObject with a statusCode of 404 doesn't return a Failure" in new scope {
        val storedFileWithoutStorageKey = mkFile("test-file", "")
        val supportingMaterials = mkSupportingMaterials(storedFileWithoutStorageKey)
        val fromItem = mkItem(supportingMaterials = supportingMaterials)
        val toItem = incVersion(fromItem)
        s3.copyAsset(any[String], any[String]) throws {
          val e = new AmazonS3Exception("test-error")
          e.setStatusCode(404)
          e
        }
        service.cloneStoredFiles(fromItem, toItem).isSuccess must_== true
      }

      "ensure that to-item has a version" in new scope {
        val toItem = mkItem(randomItemIdWithoutVersion)
        val fromItem = mkItem(randomItemIdWithVersion)
        service.cloneStoredFiles(fromItem, toItem) must_== Failure(MissingVersionFromId(toItem.id))
      }

      "ensure than from-item has a version" in new scope {
        val toItem = mkItem(randomItemIdWithVersion)
        val fromItem = mkItem(randomItemIdWithoutVersion)
        service.cloneStoredFiles(fromItem, toItem) must_== Failure(MissingVersionFromId(fromItem.id))
      }

      "not fail when item has no files" in new scope {
        val toItem = mkItem()
        val fromItem = incVersion(toItem)
        service.cloneStoredFiles(fromItem, toItem) must_== Success(toItem)
      }

      "not fail when file in supportingMaterials has no storageKey" in new scope {
        val storedFileWithoutStorageKey = mkFile("test-file", "")
        val supportingMaterials = mkSupportingMaterials(storedFileWithoutStorageKey)
        val fromItem = mkItem(supportingMaterials = supportingMaterials)
        val toItem = incVersion(fromItem)
        service.cloneStoredFiles(fromItem, toItem).isSuccess must_== true
      }

      "fail when file in data.files has no storageKey" in new scope {
        val storedFileWithoutStorageKey = mkFile("test-file", "")
        val data = mkData(storedFileWithoutStorageKey)
        val fromItem = mkItem(data = data)
        val toItem = incVersion(fromItem)
        service.cloneStoredFiles(fromItem, toItem).isFailure must_== true
      }

      "not fail when file in supportingMaterials has storageKey as fileName" in new scope {
        val storedFileWithoutNameEqualsStorageKey = mkFile("test-file", "test-file")
        val supportingMaterials = mkSupportingMaterials(storedFileWithoutNameEqualsStorageKey)
        val fromItem = mkItem(supportingMaterials = supportingMaterials)
        val toItem = incVersion(fromItem)
        service.cloneStoredFiles(fromItem, toItem).isSuccess must_== true
      }

      def mkKey(id: VersionedId[ObjectId], r: Resource, prefix: Option[String] = None) = {
        val n = prefix.map { p => s"$p/${r.name}" }.getOrElse(r.name)
        StoredFile.storageKey(id.id, id.version.get, n, r.storedFiles.head.name)
      }

      def assertSupportingMaterial(msg: String, currentKey: String, expectedCopyKey: Option[Item => String] = None) = {
        msg in new scope {
          val supportingMaterials = mkSupportingMaterials(mkFile("test-file", currentKey))
          val fromItem = mkItem(supportingMaterials = supportingMaterials)
          val expectedKey = expectedCopyKey.map(fn => fn(fromItem)).getOrElse(currentKey)
          val toItem = incVersion(fromItem)
          service.cloneStoredFiles(fromItem, toItem)
          there was one(s3).copyAsset(
            expectedKey,
            mkKey(toItem.id, toItem.supportingMaterials(0), Some("materials")))
        }
      }

      assertSupportingMaterial("uses existing storageKey if defined", "some/existing/key")

      assertSupportingMaterial("uses default storageKey if empty", "", Some((from) => {
        mkKey(from.id, from.supportingMaterials(0), Some("materials"))
      }))

      assertSupportingMaterial("uses default storageKey if null", null, Some((from) => {
        mkKey(from.id, from.supportingMaterials(0), Some("materials"))
      }))

      assertSupportingMaterial("uses default storageKey if same as filename", "test-file", Some((from) => {
        mkKey(from.id, from.supportingMaterials(0), Some("materials"))
      }))

      "fail when file in data.files has storageKey as fileName" in new scope {
        val storedFileWithoutNameEqualsStorageKey = mkFile("test-file", "test-file")
        val data = mkData(storedFileWithoutNameEqualsStorageKey)
        val fromItem = mkItem(data = data)
        val toItem = incVersion(fromItem)
        service.cloneStoredFiles(fromItem, toItem).isFailure must_== true
      }

      "prefer file from data.files when file with same name exists in playerDefinition" in new scope {
        val fileInData = mkFile("test-file", "file from data")
        val data = mkData(fileInData)
        val fileInPlayerDefinition = mkFile("test-file", "file from playerdefinition")
        val fromItem = mkItem(data = data, files = Seq(fileInPlayerDefinition))
        val toItem = incVersion(fromItem)
        service.cloneStoredFiles(fromItem, toItem)
        there was one(s3).copyAsset("file from data", s"${itemIdToPath(toItem)}/test-resource/test-file")
        there was no(s3).copyAsset("file from playerdefinition", s"${itemIdToPath(toItem)}/test-resource/test-file")
      }

      "prefer file from supportingMaterials when file with same name exists in playerDefinition" in new scope {
        val fileInSupportingMaterials = mkFile("test-file", "file from supportingMaterials")
        val supportingMaterials = mkSupportingMaterials(fileInSupportingMaterials)
        val fileInPlayerDefinition = mkFile("test-file", "file from playerdefinition")
        val fromItem = mkItem(supportingMaterials = supportingMaterials, files = Seq(fileInPlayerDefinition))
        val toItem = incVersion(fromItem)
        service.cloneStoredFiles(fromItem, toItem)
        there was one(s3).copyAsset("file from supportingMaterials", s"${itemIdToPath(toItem)}/materials/test-resource/test-file")
        there was no(s3).copyAsset("file from playerdefinition", s"${itemIdToPath(toItem)}/test-resource/test-file")
      }

      "copy file in playerDefinition" in new scope {
        val fileInPlayerDefinition = mkFile("test-file", "file from playerdefinition")
        val fromItem = mkItem(files = Seq(fileInPlayerDefinition))
        val toItem = incVersion(fromItem)
        service.cloneStoredFiles(fromItem, toItem)
        there was one(s3).copyAsset(s"${itemIdToPath(fromItem)}/data/test-file", s"${itemIdToPath(toItem)}/data/test-file")
      }

      "copy file in data.files" in new scope {
        val fileInData = mkFile("test-file", "file from data")
        val data = mkData(fileInData)
        val fromItem = mkItem(data = data)
        val toItem = incVersion(fromItem)
        service.cloneStoredFiles(fromItem, toItem)
        there was one(s3).copyAsset("file from data", s"${itemIdToPath(toItem)}/test-resource/test-file")
      }

      "copy file in supportingMaterials" in new scope {
        val fileInSupportingMaterials = mkFile("test-file", "file from supportingMaterials")
        val supportingMaterials = mkSupportingMaterials(fileInSupportingMaterials)
        val fromItem = mkItem(supportingMaterials = supportingMaterials)
        val toItem = incVersion(fromItem)
        service.cloneStoredFiles(fromItem, toItem)
        there was one(s3).copyAsset("file from supportingMaterials", s"${itemIdToPath(toItem)}/materials/test-resource/test-file")
      }

      "when the item contains same file in playerDefinition & supportingMaterials" should {

        trait sameFileScope extends scope {
          val fileInSupportingMaterials = mkFile("test-file", "file from supportingMaterials")
          val supportingMaterials = mkSupportingMaterials(fileInSupportingMaterials)
          val fileInPlayerDefinition = mkFile("test-file", "file from playerdefinition")
          val fromItem = mkItem(files = Seq(fileInPlayerDefinition), supportingMaterials = supportingMaterials)
          val toItem = incVersion(fromItem)
          service.cloneStoredFiles(fromItem, toItem)
        }

        "copy file in playerDefinition" in new sameFileScope {
          there was one(s3).copyAsset(s"${itemIdToPath(fromItem)}/data/test-file", s"${itemIdToPath(toItem)}/data/test-file")
        }

        "copy file in supportingMaterials" in new sameFileScope {
          there was one(s3).copyAsset("file from supportingMaterials", s"${itemIdToPath(toItem)}/materials/test-resource/test-file")
        }

      }

      trait missingScope extends scope {
        val missing = mock[AmazonS3Exception]
        missing.getStatusCode().returns(404)
        s3.copyAsset(any[String], any[String]) throws (missing)

        val toItem = mkItem(randomItemIdWithVersion)
        val fromItem = mkItem(randomItemIdWithVersion)
        val result = service.cloneStoredFiles(fromItem, toItem)
      }

      "when there are missing files from s3" should {

        "return success" in new missingScope {
          result must haveClass[Success[Seq[CloneFileResult], Item]]
        }

      }

    }

    "delete" should {

      "remove a file identified by the storageKey" in new scope {
        service.delete("123")
        there was one(s3).deleteAsset("123")
      }

    }

  }
}
