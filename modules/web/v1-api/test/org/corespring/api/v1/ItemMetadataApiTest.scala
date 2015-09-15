package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.legacy.ServiceLookup
import org.corespring.models.Organization
import org.corespring.models.auth.AccessToken
import org.corespring.models.metadata.{ Metadata, MetadataSet, SchemaMetadata }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.{ OrganizationService, ContentCollectionService }
import org.corespring.services.auth.AccessTokenService
import org.corespring.services.metadata.{ MetadataService, MetadataSetService }
import org.corespring.test.JsonAssertions
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.{ BeforeAfter, Specification }
import org.specs2.specification.Scope
import play.api.GlobalSettings
import play.api.test.{ PlaySpecification, FakeApplication, FakeRequest }

import scala.collection.mutable

class ItemMetadataApiTest
  extends PlaySpecification
  with Mockito
  with JsonAssertions {

  var originals: mutable.Map[String, Any] = mutable.Map.empty

  val archiveId = ObjectId.get

  val mockContentCollectionService = {
    val m = mock[ContentCollectionService]
    m.archiveCollectionId returns archiveId
    m
  }

  val mockedOrg = Organization("testorg", id = ObjectId.get)

  val mockAccessTokenService = {
    val m = mock[AccessTokenService]
    m.findById(any[String]) returns Some(AccessToken(mockedOrg.id, None, "test_token", expirationDate = DateTime.now().plusHours(24), neverExpire = true))
    m
  }

  val mockOrgService = {
    val m = mock[OrganizationService]
    m.findOneById(any[ObjectId]) returns Some(mockedOrg)
    m
  }

  private trait scope extends Scope with BeforeAfter {
    override def after: Any = {
      ServiceLookup.contentCollectionService = originals.get("contentCollection").get.asInstanceOf[ContentCollectionService]
      ServiceLookup.accessTokenService = originals.get("accessToken").get.asInstanceOf[AccessTokenService]
    }

    override def before: Any = {
      originals.put("contentCollection", ServiceLookup.contentCollectionService)
      originals.put("accessToken", ServiceLookup.accessTokenService)
      originals.put("org", ServiceLookup.orgService)
      ServiceLookup.contentCollectionService = mockContentCollectionService
      ServiceLookup.accessTokenService = mockAccessTokenService
      ServiceLookup.orgService = mockOrgService
    }
  }

  val mockGlobal = new GlobalSettings {}

  "ItemMetadataApi" should {

    "get metadata for an item" in new scope {
      running(FakeApplication(withGlobal = Some(mockGlobal))) {

        val orgId = ObjectId.get
        val setId = ObjectId.get
        val itemId = VersionedId(ObjectId.get)

        val set = MetadataSet("key", "url", "label", false, Seq(SchemaMetadata("schema_key")), setId)
        val setService: MetadataSetService = mock[MetadataSetService]

        import org.mockito.Matchers._

        setService.list(anyObject()) returns Seq(set)

        val metadata = Metadata("demo_key", Map("schema_key" -> "schema_value"))
        val metadataService: MetadataService = mock[MetadataService]
        metadataService.get(anyObject(), anyObject()) returns Seq(metadata)

        val api: ItemMetadataApi = new ItemMetadataApi(metadataService, setService)

        val json = api.get(itemId)(FakeRequest("", "?access_token=test_token"))

        val expected =
          s"""
          |[
          |{
          | "metadataKey" : "key",
          | "editorUrl" : "url",
          | "editorLabel" : "label",
          | "id" : "$setId",
          | "isPublic" : false,
          | "schema" : [ { "key" : "schema_key" } ],
          | "data" : [
          | { "schema_key" : "schema_value" }
          | ]
          |}
          |]
        """.stripMargin

        assertJsonIsEqual(expected, contentAsString(json))

      }
    }
  }

}
