package org.corespring.api.v1

import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.Organization
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.models.json.JsonFormatting
import org.corespring.models.metadata.{Metadata, MetadataSet, SchemaMetadata}
import org.corespring.platform.core.controllers.auth.{AuthorizationContext, OAuthProvider}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.services.metadata.{MetadataService, MetadataSetService}
import org.corespring.services.{OrgCollectionService, ContentCollectionService, OrganizationService}
import org.corespring.test.JsonAssertions
import org.corespring.v2.sessiondb.SessionServices
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.GlobalSettings
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification}

import scala.collection.mutable
import scalaz.Success

class ItemApiTest
  extends PlaySpecification
  with Mockito
  with JsonAssertions {

  val mockedOrg = Organization("testorg", id = ObjectId.get)

  val mockV2ItemApi = mock[org.corespring.v2.api.ItemApi]
  val mockS3service = mock[S3Service]
  val mockService = mock[ItemService]
  val mockSalatService = mock[SalatContentService[Item, _]]
  val mockMetadataSetService = mock[MetadataSetService]
  val mockOrgService = {
    val m = mock[OrganizationService]
    m.findOneById(any[ObjectId]) returns Some(mockedOrg)
    m
  }
  val mockOrgCollectionService = mock[OrgCollectionService]
  val mockSessionServices = mock[SessionServices]
  val mockItemTransformer = mock[ItemTransformer]
  val mockJsonFormatting = mock[JsonFormatting]
  val mockOAuthProvider = {
    val m = mock[OAuthProvider]
    m.getAuthorizationContext(any[String]) returns Success(
      AuthorizationContext(None, mockedOrg, Permission.Write, false))
  }
  val mockContext = mock[Context]


  private trait scope extends Scope

  val mockGlobal = new GlobalSettings {}

  "ItemApi" should {

    "update" should {

      "replace storageKeys in data with keys from db item" in new scope {
        running(FakeApplication(withGlobal = Some(mockGlobal))) {

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
            mockOAuthProvider,
            mockContext
          )

          val itemId = VersionedId(ObjectId.get)


          val item = api.update(itemId)(FakeRequest("", "?access_token=test_token"))

          item must_== expectedItem

      }
    }
      }
  }

}
