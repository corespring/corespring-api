package tests.api.v1

import play.api.libs.json.{JsNull, Json, JsValue}
import play.api.test._
import play.api.test.Helpers._
import scala._
import play.api.test.FakeHeaders
import play.api.mvc.AnyContentAsJson
import scala.Some
import tests.BaseTest
import com.mongodb.casbah.Imports._
import com.novus.salat._
import controllers.auth.Permission
import models.User
import models.UserOrg


class CollectionApiTest extends BaseTest {

  val INITIAL_COLLECTION_SIZE : Int = 7

  //todo: fix these. occasionally, when ItemApiTest is run before this, a collection will be created (when storing to an item without a collection. a default collection is generated.). We should clear the database for each test that is run
  "list all collections" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val collections = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    collections.size must beGreaterThanOrEqualTo(INITIAL_COLLECTION_SIZE)
  }

  "list all collections skipping the first result" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections?access_token=%s&sk=1".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val collections = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    collections.size must beGreaterThanOrEqualTo(INITIAL_COLLECTION_SIZE - 1)
  }

  "list all collections limit results to 1" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections?access_token=%s&l=1".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val collections = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    collections must have size 1
  }

  "find a collection with name 'Collection G'" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections?access_token=%s&q={\"name\":\"Collection+G\"}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val collections = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    collections must have size 1
    (collections(0) \ "name").as[String] must beEqualTo("Collection G")
  }

  val collectionId = "5001bb0ee4b0d7c9ec3210a2"
  "get a collection by id '%s'".format(collectionId) in {
    val fakeRequest = FakeRequest(GET, "/api/v1/collections/%s?access_token=%s".format(collectionId, token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val collection = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (collection \ "id").as[String] must beEqualTo(collectionId)
  }

  "create, update and delete a collection" in {
    val name = "test collection"
    User.update(
      MongoDBObject(User.userName -> "demo_user", User.orgs+"."+UserOrg.orgId -> new ObjectId("502404dd0364dc35bb393397")),
      MongoDBObject("$set" -> MongoDBObject(User.orgs+".$."+UserOrg.pval -> Permission.Write.value)),false,false,User.defaultWriteConcern)
    // create it
    val toCreate = Map("name" -> name)
    val fakeRequest = FakeRequest(POST, "/api/v1/collections?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
    val r = routeAndCall(fakeRequest)
    if (r.isEmpty) {
      failure("Failed to create collection")
    }
    val result = r.get
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val collection = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (collection \ "name").as[String] must beEqualTo(name)

    // update
    val name2 = "a new name"
    val toUpdate = Map("name" -> name2)
    val collectionId = (collection \ "id").as[String]
    val postRequest = FakeRequest(PUT, "/api/v1/collections/%s?access_token=%s".format(collectionId, token), FakeHeaders(), AnyContentAsJson(Json.toJson(toUpdate)))
    routeAndCall(postRequest) match {
      case Some(result2) => {
        status(result2) must equalTo(OK)
        charset(result2) must beSome("utf-8")
        contentType(result2) must beSome("application/json")
        val updatedCollection = Json.fromJson[JsValue](Json.parse(contentAsString(result2)))
        (updatedCollection \ "id").as[String] must beEqualTo(collectionId)
        (updatedCollection \ "name").as[String] must beEqualTo(name2)

        // delete
        val deleteRequest = FakeRequest(DELETE, "/api/v1/collections/%s?access_token=%s".format(collectionId, token))
        val Some(result3) = routeAndCall(deleteRequest)
        status(result3) must equalTo(OK)
        charset(result3) must beSome("utf-8")
        contentType(result3) must beSome("application/json")

        val Some(result4) = routeAndCall(FakeRequest(GET, "/api/v1/collections/%s?access_token=%s".format(collectionId, token)))
        status(result4) must equalTo(NOT_FOUND)
      }
      case None => failure("failed to delete collection")
    }
    User.update(
      MongoDBObject(User.userName -> "demo_user", User.orgs+"."+UserOrg.orgId -> new ObjectId("502404dd0364dc35bb393397")),
      MongoDBObject("$set" -> MongoDBObject(User.orgs+".$."+UserOrg.pval -> Permission.Read.value)),false,false,User.defaultWriteConcern)
  }


}
