package tests.api.v1

import api.v1.CollectionApi
import org.bson.types.ObjectId
import org.corespring.test.BaseTest
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import play.api.test._
import scala._


class CollectionApiTest extends BaseTest {

  val INITIAL_COLLECTION_SIZE : Int = 2

  val routes = api.v1.routes.CollectionApi


  //todo: fix these. occasionally, when ItemApiTest is run before this, a collection will be created (when storing to an item without a collection. a default collection is generated.). We should clear the database for each test that is run
  "list all collections" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections?access_token=%s".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val collections = parsed[List[JsValue]](result)
    collections.size must beGreaterThanOrEqualTo(INITIAL_COLLECTION_SIZE)
  }

  "list all collections skipping the first result" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections?access_token=%s&sk=1".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val collections = parsed[List[JsValue]](result)
    collections.size must beGreaterThanOrEqualTo(INITIAL_COLLECTION_SIZE - 1)
  }

  "list all collections limit results to 1" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections?access_token=%s&l=1".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val collections = parsed[List[JsValue]](result)
    collections must have size 1
  }

  "find a collection with name 'Demo Collection 2'" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections?access_token=%s&q={\"name\":\"Demo Collection 2\"}".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val collections = parsed[List[JsValue]](result)
    collections must have size 1
    (collections(0) \ "name").as[String] must beEqualTo("Demo Collection 2")
  }

  val collectionId = "51114b127fc1eaa866444647"
  "get a collection by id '%s'".format(collectionId) in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections/%s?access_token=%s".format(collectionId, token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val collection = parsed[JsValue](result)
    (collection \ "id").as[String] must beEqualTo(collectionId)
  }

  "create, update and delete a collection" in {
    val name = "test collection"
    // create it
    val createRequest = FakeRequest(POST, tokenize("b"), FakeHeaders(), AnyContentAsJson(Json.toJson(Map("name" -> name))))
    val createResult = CollectionApi.createCollection()(createRequest)
    assertResult(createResult)
    val collection = parsed[JsValue](createResult)
    (collection \ "name").as[String] must beEqualTo(name)

    // update
    val name2 = "a new name"
    val toUpdate = Map("name" -> name2)
    val collectionId = (collection \ "id").as[String]
    val oid = new ObjectId(collectionId)
    val postRequest = FakeRequest(PUT, tokenize("b"), FakeHeaders(), AnyContentAsJson(Json.toJson(toUpdate)))
    val updateResult = CollectionApi.updateCollection(oid)(postRequest)

    assertResult(updateResult)
    val updatedCollection = parsed[JsValue](updateResult)
    (updatedCollection \ "id").as[String] must beEqualTo(collectionId)
    (updatedCollection \ "name").as[String] must beEqualTo(name2)

    val deleteRequest = FakeRequest(DELETE, "s/%s?access_token=%s".format(collectionId, token))
    val deleteResult = CollectionApi.deleteCollection(oid)(deleteRequest)
    assertResult(deleteResult)

    val getResult = CollectionApi.getCollection(oid)(FakeRequest(GET, tokenize("b")))
    status(getResult)  === NOT_FOUND

  }


}
