package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ BaseFile, VirtualFile, Resource, StoredFile }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.JsonAssertions
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.test.PlaySpecification

import scalaz.Success

class ItemApiItemValidationTest
  extends PlaySpecification
  with Mockito
  with JsonAssertions {

  val itemId = VersionedId(ObjectId.get)
  val collectionId = ObjectId.get

  val storedFileWithoutStorageKey = StoredFile(
    name = "mc008-3.jpg",
    contentType = BaseFile.ContentTypes.JPEG,
    isMain = false)

  val storedFile = storedFileWithoutStorageKey.copy(
    storageKey = "52a5ed3e3004dc6f68cdd9fc/0/data/mc008-3.jpg")

  val virtualFile = VirtualFile(
    name = "qti.xml",
    contentType = BaseFile.ContentTypes.XML,
    isMain = true,
    content = "<qti></qti>")

  val dbItem = Item(
    collectionId = collectionId.toString,
    id = itemId,
    data = Some(Resource(name = "data", files = Seq(storedFile, virtualFile))),
    supportingMaterials = Seq(Resource(name = "sm-1", files = Seq(virtualFile, storedFile))))

  val sut: ItemApiItemValidation = new ItemApiItemValidation()

  private trait scope extends Scope

  "ItemApiItemValidation" should {

    "validateItem" should {

      "insert storageKeys from db item into data.files" in new scope {

        val initialResource = Resource(name = "data", files = Seq(storedFileWithoutStorageKey, virtualFile))
        val expectedResource = initialResource.copy(files = Seq(storedFile, virtualFile))

        val item = Item(
          collectionId = collectionId.toString,
          id = itemId,
          data = Some(initialResource))

        val expected = item.copy(data = Some(expectedResource))

        sut.validateItem(dbItem, item) must_== Success(expected)
      }

      "insert storageKeys from db item into data.supportingMaterials" in new scope {

        val initialResource = Resource(name = "sm-1", files = Seq(virtualFile, storedFileWithoutStorageKey))
        val expectedResource = initialResource.copy(files = Seq(virtualFile, storedFile))

        val item = Item(
          collectionId = collectionId.toString,
          id = itemId,
          supportingMaterials = Seq(initialResource))

        val expected = item.copy(supportingMaterials = Seq(expectedResource))

        sut.validateItem(dbItem, item) must_== Success(expected)
      }
    }

  }

}
