package tests.api.v1

import api.v1.CollectionApi
import org.bson.types.ObjectId
import org.corespring.test.BaseTest
import play.api.libs.json.{JsNumber, Json, JsValue}
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import play.api.test._
import scala._
import org.specs2.mutable.BeforeAfter
import org.specs2.specification.Step
import tests.helpers.models._
import play.api.test.FakeHeaders
import scala.Some
import play.api.mvc.AnyContentAsJson
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.auth.Permission


class CollectionApiTest extends BaseTest {

  val INITIAL_COLLECTION_SIZE : Int = 2
  val orgId = "51114b307fc1eaa866444648"
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

//  s"return itemType values and counts for $collectionId" in {
//    skipped("these results are not accurate")
//    val fakeRequest = FakeRequest(GET, s"/api/v1/collections/$collectionId/fieldValues/itemType?access_token=$token")
//    val Some(result) = route(fakeRequest)
//    val json = parsed[JsValue](result)
//    (json \ "Constructed Response - Short Answer") match {
//      case jsNumber: JsNumber => jsNumber.value must be equalTo 1.0
//      case _ => failure("Json was missing constructed response value")
//    }
//    (json \ "Multiple Choice") match {
//      case jsNumber: JsNumber => jsNumber.value must be equalTo 5.0
//      case _ => failure("Json was missing multiple choice value")
//    }
//    (json \ "Text with Questions") match {
//      case jsNumber: JsNumber => jsNumber.value must be equalTo 1.0
//      case _ => failure("Json was missing text with questions value")
//    }
//  }
//
//  s"return contributor values and counts for $collectionId" in {
//    val fakeRequest = FakeRequest(GET, s"/api/v1/collections/$collectionId/fieldValues/contributor?access_token=$token")
//    val Some(result) = route(fakeRequest)
//    val json = parsed[JsValue](result)
//    (json \ "New England Common Assessment Program") match {
//      case jsNumber: JsNumber => jsNumber.value must be equalTo 2.0
//      case _ => failure("Json was msising New England Common Assessment Program value")
//    }
//    (json \ "New York State Education Department") match {
//      case jsNumber: JsNumber => jsNumber.value must be equalTo 1.0
//      case _ => failure("Json was missing New York State Education Department value")
//    }
//    (json \ "State of New Jersey Department of Education") match {
//      case jsNumber: JsNumber => jsNumber.value must be equalTo 1.0
//      case _ => failure("Json was missing State of New Jersey Department of Education value")
//    }
//  }

  "return bad request for invalid fieldValue property" in {
    val fakeRequest = FakeRequest(GET, s"/api/v1/collections/$collectionId/fieldValues/NotLegit?access_token=$token")
    val Some(result) = route(fakeRequest)
    status(result) must be equalTo BAD_REQUEST
  }

  "create, update and delete a collection" in {
    val name = "test collection"
    // create it
    val createRequest = FakeRequest(POST, tokenize("b"), FakeHeaders(), AnyContentAsJson(Json.toJson(Map("name" -> name))))
    val createResult = CollectionApi.createCollection()(createRequest)
    assertResult(createResult)
    val collection = parsed[JsValue](createResult)
    (collection \ "name").as[String] must beEqualTo(name)
    (collection \ "ownerOrgId").as[String] must beEqualTo(orgId)


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



  "share items to a collection" in new CollectionSharingScope {
    // share some existing items with a collection
    val jsonBody = Map("items" -> collectionB1ItemIds.map(_.toString()))
    val shareItemsReq =
      FakeRequest(PUT, s"/api/v1/collections/$collectionA1/add-items?access_token=%s".format(accessTokenA),
        FakeHeaders(),
        AnyContentAsJson(Json.toJson(jsonBody)))

    val shareItemsResult = CollectionApi.shareItemsWithCollection(collectionA1)(shareItemsReq)
    assertResult(shareItemsResult)

  }

  "add filtered items to a collection" in {
    // this is to support a user searching for a set of items, then adding that set of items to a collection
    println("add filtered items to a collection")
    pending
  }




}


trait CollectionSharingScope extends BeforeAfter {

  lazy val organizationA = OrganizationHelper.create("A")

  lazy val collectionA1 = CollectionHelper.create(organizationA)
  lazy val userA = UserHelper.create(organizationA)
  lazy val accessTokenA = AccessTokenHelper.create(organizationA, userA.userName)

  val collectionA1ItemIds = 1.to(3).map(i => ItemHelper.create(collectionA1))

  lazy val organizationB = OrganizationHelper.create("B")
  lazy val collectionB1 = CollectionHelper.create(organizationB)
  lazy val collectionB2 = CollectionHelper.create(organizationB)
  lazy val userB = UserHelper.create(organizationB)
  lazy val accessTokenB = AccessTokenHelper.create(organizationB, userB.userName)

  val collectionB1ItemIds = 1.to(3).map(i => ItemHelper.create(collectionB1))
  val collectionB2ItemIds = 1.to(3).map(i => ItemHelper.create(collectionB2))


  // give organization A access to collection B1
  ContentCollection.addOrganizations(Seq((organizationA, Permission.Read)), collectionB1)



  def before : Unit = {
  }

  def after : Unit = {
    println(s"[CollectionSharingScope] deleting: fixture data")


    AccessTokenHelper.delete(accessTokenA)
    collectionA1ItemIds.foreach(ItemHelper.delete(_))
    CollectionHelper.delete(collectionA1)
    UserHelper.delete(userA.id)
    OrganizationHelper.delete(organizationA)


    AccessTokenHelper.delete(accessTokenB)
    collectionB1ItemIds.foreach(ItemHelper.delete(_))
    collectionB2ItemIds.foreach(ItemHelper.delete(_))
    CollectionHelper.delete(collectionB1)
    CollectionHelper.delete(collectionB2)
    UserHelper.delete(userB.id)
    OrganizationHelper.delete(organizationB)

  }
}