package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.metadata.{ Metadata, MetadataSet, SchemaMetadata }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.metadata.{ MetadataService, MetadataSetService }
import org.corespring.v2.actions.V2ActionsFactory
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext
import scalaz.Success

class MetadataApiTest extends V2ApiSpec {

  case class apiScope(
    metadata: Map[String, Map[String, String]] = Map.empty[String, Map[String, String]],
    metadataSetId: ObjectId = new ObjectId()) extends Scope {

    val metadatas = metadata.map {
      case (key, metadata) => {
        MetadataSet(editorLabel = "label", editorUrl = "url", metadataKey = key,
          schema = metadata.map {
            case (key, value) => {
              SchemaMetadata(value)
            }
          }.toSeq)
      }
    }.toSeq

    val metadataService = {
      val m = mock[MetadataService]
      m.get(any[VersionedId[ObjectId]], any[Seq[String]]) answers { (args, _) =>
        {
          val argArray = args.asInstanceOf[Array[Object]]
          val keys = argArray(1).asInstanceOf[Seq[String]]
          keys.map(key => metadata.get(key).map(Metadata(key, _))).flatten
        }
      }
      m
    }

    val metadataSetService = {
      val m = mock[MetadataSetService]
      m.list(any[ObjectId]) returns metadatas
      m.findOneById(metadataSetId) returns metadatas.headOption
      m.create(any[ObjectId], any[MetadataSet]) answers { (args, _) =>
        {
          val argArray = args.asInstanceOf[Array[Object]]
          Success(argArray(1).asInstanceOf[MetadataSet])
        }
      }

      m.update(any[MetadataSet]) answers { (args, _) =>
        {
          val argArray = args.asInstanceOf[Array[Object]]
          Success(argArray(0).asInstanceOf[MetadataSet])
        }
      }
      m
    }

    val v2ApiContext = V2ApiExecutionContext(ExecutionContext.global)
    val metadataApi = new MetadataApi(V2ActionsFactory.apply(), metadataSetService, metadataService, v2ApiContext)
  }

  val metadata = Map("this" -> Map("is" -> "the"), "form" -> Map("of" -> "metadata"))

  "getByItemId" should {

    val itemId = new VersionedId[ObjectId](new ObjectId(), Some(0))

    "return 200" in new apiScope(metadata = metadata) {
      status(metadataApi.getByItemId(itemId)(FakeRequest())) must be equalTo (OK)
    }

    "return metadata as json in body" in new apiScope(metadata = metadata) {
      val json = contentAsJson(metadataApi.getByItemId(itemId)(FakeRequest()))
      json.as[Seq[JsObject]].map(metadata =>
        (metadata \ "metadataKey").as[String] -> (metadata \ "data").as[Map[String, String]]).toMap must be equalTo metadata
    }
  }

  "get" should {

    val metadata = Map("this" -> Map("is" -> "the"), "form" -> Map("of" -> "metadata"))

    "return 200" in new apiScope(metadata = metadata) {
      status(metadataApi.get()(FakeRequest())) must be equalTo (OK)
    }

    "return metadata as json in body" in new apiScope(metadata = metadata) {
      val json = contentAsJson(metadataApi.get()(FakeRequest()))
      json.as[Seq[JsObject]].map(metadata =>
        (metadata \ "metadataKey").as[String] -> (metadata \ "schema").as[Seq[Map[String, String]]].head).toMap must be equalTo metadata.map {
        case (key, value) => key -> value.map { case (key, value) => "key" -> value }
      }.toMap
    }
  }

  "create" should {

    "with empty json" should {
      "return 400" in new apiScope() {
        status(metadataApi.create()(FakeRequest().withJsonBody(Json.obj()))) must be equalTo (BAD_REQUEST)
      }
    }

    "with valid MetadataSet json body" should {

      val json = Json.obj(
        "metadataKey" -> "key",
        "editorUrl" -> "url",
        "editorLabel" -> "label")

      "return 201" in new apiScope() {
        status(metadataApi.create()(FakeRequest().withJsonBody(Json.obj()))) must be equalTo (BAD_REQUEST)
      }

      "return MetadataSet json in response body" in new apiScope() {
        val response = contentAsJson(metadataApi.create()(FakeRequest().withJsonBody(json)))
        (response \ "metadataKey") must be equalTo (json \ "metadataKey")
        (response \ "editorUrl") must be equalTo (json \ "editorUrl")
        (response \ "editorLabel") must be equalTo (json \ "editorLabel")
        (response \ "id").asOpt[String] must not beEmpty
      }
    }
  }

  "update" should {

    val metadataSetId = new ObjectId()

    "with empty json" should {

      "return 400" in new apiScope(metadata = metadata, metadataSetId = metadataSetId) {
        status(metadataApi.update(metadataSetId)(FakeRequest().withJsonBody(Json.obj()))) must be equalTo (BAD_REQUEST)
      }
    }

    val json = Json.obj(
      "metadataKey" -> "new key",
      "editorUrl" -> "new url",
      "editorLabel" -> "new label")

    "with valid MetadataSet json body" should {

      "return 200" in new apiScope(metadata = metadata, metadataSetId = metadataSetId) {
        status(metadataApi.update(metadataSetId)(FakeRequest().withJsonBody(json))) must be equalTo (OK)
      }

      "return updated MetadataSet json in response body" in new apiScope(metadata = metadata, metadataSetId = metadataSetId) {
        val response = contentAsJson(metadataApi.update(metadataSetId)(FakeRequest().withJsonBody(json)))
        (response \ "metadataKey") must be equalTo (json \ "metadataKey")
        (response \ "editorUrl") must be equalTo (json \ "editorUrl")
        (response \ "editorLabel") must be equalTo (json \ "editorLabel")
      }
    }
  }

  "getById" should {

    val metadataSetId = new ObjectId()

    "return 200" in new apiScope(metadata = metadata, metadataSetId = metadataSetId) {
      status(metadataApi.getById(metadataSetId)(FakeRequest())) must be equalTo (OK)
    }

    "return MetadataSet json in response body" in new apiScope(metadata = metadata, metadataSetId = metadataSetId) {
      val response = contentAsJson(metadataApi.getById(metadataSetId)(FakeRequest()))
      (response \ "metadataKey").asOpt[JsValue] must not beEmpty;
      (response \ "editorUrl").asOpt[JsValue] must not beEmpty;
      (response \ "editorLabel").asOpt[JsValue] must not beEmpty
    }
  }
}
