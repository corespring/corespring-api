package org.corespring.api.v1

import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.auth.Permission
import org.corespring.models.item.resource.{ Resource, StoredFile }
import org.corespring.models.item.{ FieldValue, Item }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Organization, Standard, Subject }
import org.corespring.platform.core.controllers.auth.{ AuthorizationContext, OAuthProvider }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.services.metadata.MetadataSetService
import org.corespring.services.{ OrgCollectionService, OrganizationService }
import org.corespring.test.JsonAssertions
import org.corespring.v2.sessiondb.{ SessionService, SessionServices }
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.GlobalSettings
import play.api.libs.json.Json
import play.api.test.{ FakeApplication, FakeRequest, PlaySpecification }

import org.mockito.Matchers._

import scala.concurrent.ExecutionContext
import scalaz.Success

class ItemApiTest
  extends PlaySpecification
  with Mockito
  with JsonAssertions {

  private trait scope extends Scope {
    val itemId = VersionedId(ObjectId.get)
    val collectionId = ObjectId.get

    val defaultStoredFile = StoredFile(
      name = "mc008-3.jpg",
      contentType = "image/jpeg",
      isMain = false,
      storageKey = "52a5ed3e3004dc6f68cdd9fc/0/data/mc008-3.jpg")

    def storedFile = defaultStoredFile

    val defaultDbItem = Item(
      collectionId = collectionId.toString,
      id = itemId,
      data = Some(Resource(name = "data", files = Seq(storedFile))),
      supportingMaterials = Seq(Resource(name = "sm-1", files = Seq(storedFile))))

    def dbItem = defaultDbItem

    val mockedOrg = Organization("testorg", id = ObjectId.get)

    val mockV2ItemApi = mock[org.corespring.v2.api.ItemApi]
    val mockS3service = mock[S3Service]
    val mockService = {
      val m = mock[ItemService]
      m.isAuthorized(any[ObjectId], any[VersionedId[ObjectId]], any[Permission]) returns Success()
      m.findOneById(any[VersionedId[ObjectId]]) returns Some(dbItem)
      m.save(any[Item], any[Boolean]) returns Success(dbItem.id)
      m
    }
    val mockSalatService = mock[SalatContentService[Item, _]]
    val mockMetadataSetService = mock[MetadataSetService]
    val mockOrgService = {
      val m = mock[OrganizationService]
      m.findOneById(any[ObjectId]) returns Some(mockedOrg)
      m
    }
    val mockOrgCollectionService = mock[OrgCollectionService]
    val mockSessionServices = SessionServices(mock[SessionService], mock[SessionService])
    val mockItemTransformer = {
      val m = mock[ItemTransformer]
      m.updateV2Json(any[Item]) answers (item => item.asInstanceOf[Item])
    }
    val mockOAuthProvider = {
      val m = mock[OAuthProvider]
      m.getAuthorizationContext(any[String]) returns Success(
        AuthorizationContext(None, mockedOrg, Permission.Write, false))
    }
    val mockContext = mock[Context]

    val mockJsonFormatting = new JsonFormatting {
      override def findStandardByDotNotation: (String) => Option[Standard] = _ => None

      override def rootOrgId: ObjectId = ObjectId.get

      override def fieldValue: FieldValue = FieldValue()

      override def findSubjectById: (ObjectId) => Option[Subject] = _ => None
    }

    val mockExecutionContext = V1ApiExecutionContext(ExecutionContext.global)

    val mockItemValidation = spy(new ItemApiItemValidation)

    val api: ItemApi = new ItemApi(
      mockV2ItemApi,
      mockS3service,
      mockService,
      mockSalatService,
      mockMetadataSetService,
      mockOrgService,
      mockOrgCollectionService,
      mockSessionServices,
      mockItemTransformer,
      mockJsonFormatting,
      mockItemValidation,
      mockExecutionContext,
      mockOAuthProvider,
      mockContext)

    val mockGlobal = new GlobalSettings {}

    val inputItem = Json.obj(
      "_id" -> Json.obj(
        "_id" -> Json.obj(
          "$oid" -> itemId.id.toString),
        "version" -> itemId.version),
      "collectionId" -> collectionId.toString,
      "data" -> Json.obj(
        "name" -> "data",
        "files" -> Json.arr(
          Json.obj(
            "contentType" -> "image/jpeg",
            "isMain" -> false,
            "name" -> "mc008-3.jpg"))))
  }

  "ItemApi" should {

    "update" should {

      "call itemService.save with createNewVersion=true when item is published and has sessions" in new scope {

        override def dbItem = defaultDbItem.copy(published = Some(true))
        mockSessionServices.main.sessionCount(any) returns 1

        running(FakeApplication(withGlobal = Some(mockGlobal))) {

          api.update(itemId)(FakeRequest("", "?access_token=test_token").withJsonBody(inputItem))

          //eq(true) doesn't work, see https://github.com/etorreborre/specs2/issues/361
          there was one(mockService).save(any[Item], ===(true))
        }
      }
    }

  }

}
