package api.v1

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.test.FakeRequest
import utils.JsonAssertions
import org.corespring.test.PlaySingleton
import org.corespring.platform.core.models.metadata.{Metadata, SchemaMetadata, MetadataSet}
import org.corespring.platform.core.services.metadata.{MetadataService, MetadataSetService}

class ItemMetadataApiTest extends Specification with Mockito with JsonAssertions{

  PlaySingleton.start()

  "ItemMetadataApi" should {

    "get metadata for an item" in {

      val orgId = ObjectId.get
      val setId = ObjectId.get
      val itemId = VersionedId(ObjectId.get)

      val set = MetadataSet("key", "url", "label", false, Seq(SchemaMetadata("schema_key")), setId)
      val setService : MetadataSetService = mock[MetadataSetService]

      import org.mockito.Matchers._

      setService.list(anyObject()) returns Seq(set)

      val metadata = Metadata("demo_key", Map("schema_key" -> "schema_value"))
      val metadataService : MetadataService = mock[MetadataService]
      metadataService.get(anyObject(), anyObject()) returns Seq(metadata)

      val api : ItemMetadataApi = new ItemMetadataApi(metadataService, setService)


      val json = api.get( itemId )(FakeRequest("", "?access_token=test_token"))

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

      import play.api.test.Helpers._
      assertJsonIsEqual(expected, contentAsString(json))

    }
  }
}
