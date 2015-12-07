package org.corespring.api.v1

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.item.resource.{ StoredFile, Resource }
import org.corespring.models.{ Subject, Standard, Organization }
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ FieldValue, Item }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.metadata.{ Metadata, MetadataSet, SchemaMetadata }
import org.corespring.platform.core.controllers.auth.{ AuthorizationContext, OAuthProvider }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.services.metadata.{ MetadataService, MetadataSetService }
import org.corespring.services.{ OrgCollectionService, ContentCollectionService, OrganizationService }
import org.corespring.test.JsonAssertions
import org.corespring.v2.sessiondb.SessionServices
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.GlobalSettings
import play.api.libs.json.Json
import play.api.test.{ FakeApplication, FakeRequest, PlaySpecification }

import scala.collection.mutable
import scalaz.Success

class ItemApiItemValidationTest
  extends PlaySpecification
  with Mockito
  with JsonAssertions {

  val itemId = VersionedId(ObjectId.get)
  val collectionId = ObjectId.get

  val storedFileWithoutStorageKey = StoredFile(
    name = "mc008-3.jpg",
    contentType = "image/jpeg",
    isMain = false)

  val storedFile = storedFileWithoutStorageKey.copy(
    storageKey = "52a5ed3e3004dc6f68cdd9fc/0/data/mc008-3.jpg")

  val dbItem = Item(
    collectionId = collectionId.toString,
    id = itemId,
    data = Some(Resource(name = "data", files = Seq(storedFile))),
    supportingMaterials = Seq(Resource(name = "sm-1", files = Seq(storedFile))))

  val sut: ItemApiItemValidation = new ItemApiItemValidation()

  private trait scope extends Scope

  "ItemApiItemValidation" should {

    "validateItem" should {

      "insert storageKeys from db item into data.files" in new scope {

        val item = Item(
          collectionId = collectionId.toString,
          id = itemId,
          data = Some(Resource(name = "data", files = Seq(storedFileWithoutStorageKey))))

          val expected = Item(
            collectionId = collectionId.toString,
            id = itemId,
            data = Some(Resource(name = "data", files = Seq(storedFile))))

         sut.validateItem(dbItem, item) must_== Success(expected)
      }

      "insert storageKeys from db item into data.supportingMaterials" in new scope {
        val item = Item(
          collectionId = collectionId.toString,
          id = itemId,
          supportingMaterials = Seq(Resource(name = "sm-1", files = Seq(storedFileWithoutStorageKey))))

        val expected = Item(
          collectionId = collectionId.toString,
          id = itemId,
          supportingMaterials = Seq(Resource(name = "sm-1", files = Seq(storedFile))))

        sut.validateItem(dbItem, item) must_== Success(expected)
      }
    }

  }

}
