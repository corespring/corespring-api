package tests.api.v1

import api.v1.{ItemApi, CollectionApi}
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.core.models.{Organization, ContentCollection}
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.corespring.test.BaseTest
import org.specs2.mutable.BeforeAfter
import play.api.libs.json.{JsNumber, Json, JsValue}
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeHeaders
import play.api.test.Helpers._
import play.api.test._
import scala.Some
import scala._
import tests.helpers.models._


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
      FakeRequest(PUT, s"/api/v1/collections/$collectionA1/share-items?access_token=%s".format(accessTokenA),
        FakeHeaders(),
        AnyContentAsJson(Json.toJson(jsonBody)))

    val shareItemsResult = CollectionApi.shareItemsWithCollection(collectionA1)(shareItemsReq)
    assertResult(shareItemsResult)

  }

  "un-share items from a collection" in new CollectionSharingScope {
    // share. then un-share some items
    ContentCollection.shareItems(organizationA,collectionB1ItemIds,collectionA1) match {
      case Left(error) => failure
      case Right(savedIds) =>
        val jsonBody = Map("items" -> collectionB1ItemIds.map(_.toString()))
        val unShareItemsReq = FakeRequest(PUT, s"/api/v1/collections/$collectionA1/un-share-items?access_token=%s".format(accessTokenA),
          FakeHeaders(),
          AnyContentAsJson(Json.toJson(jsonBody)))
        val unShareItemsResult = CollectionApi.unShareItemsWithCollection(collectionA1)(unShareItemsReq)
        assertResult(unShareItemsResult)

    }




  }

  "add filtered items to a collection" in  new CollectionSharingScope {
    // this is to support a user searching for a set of items, then adding that set of items to a collection
    // share items in collection b1 that are published with collection a1...
    val query = s""" {"published":true, "collectionId":{"$$in":["$collectionB1"]} } """
    val addFilteredItemsReq = FakeRequest(GET, s"/api/v1/items?q=$query&access_token=%s".format(accessTokenA))
    val shareItemsResult = CollectionApi.shareFilteredItemsWithCollection(collectionA1,Some(query))(addFilteredItemsReq)
    assertResult(shareItemsResult)
    val response = parsed[JsNumber](shareItemsResult)
    response.toString mustEqual  "3"
    // check how many items are now available in a1. There should be 6: 3 owned by a1 and 3 shared with a1 from b1
    val listReq = FakeRequest(GET, s"/api/v1/collections/$collectionA1/items?access_token=%s".format(accessTokenA))
    val listResult = ItemApi.listWithColl(collectionA1,None,None,"10",0,10,None)(listReq)
    assertResult(listResult)
    val itemsList = parsed[List[JsValue]](listResult)
    itemsList.size must beEqualTo(6)
  }

  "find/list items should include shared items" in new CollectionSharingScope {
    ContentCollection.shareItems(organizationA,collectionB1ItemIds,collectionA1) match {
      case Left(error) => failure
      case Right(savedIds) =>
        val findRequest = FakeRequest(GET, "/api/v1/items?access_token=%s".format(accessTokenA))
        val findResult = ItemApi.list(None,None,"10",0,200,None)(findRequest)
        assertResult(findResult)
        // result should contain the b1 items
        val b1ItemsIdStrings = collectionB1ItemIds.map(_.id.toString)
        val foundItems = parsed[List[JsValue]](findResult)
        val foundItemIdStrings = foundItems.map(jsVal => ((jsVal \ "id").as[String]).split(":")(0))
        val itemsFound = b1ItemsIdStrings.filter(foundItemIdStrings.contains(_))
        itemsFound.size must beEqualTo(3)

        val listReq = FakeRequest(GET, s"/api/v1/collections/$collectionA1/items?access_token=%s".format(accessTokenA))
        val listResult = ItemApi.listWithColl(collectionA1,None,None,"10",0,200,None)(listReq)
        assertResult(listResult)
        val itemsList = parsed[List[JsValue]](listResult)
        val listItemIdStrings = foundItems.map(jsVal => ((jsVal \ "id").as[String]).split(":")(0))
        val listItemsFound = b1ItemsIdStrings.filter(listItemIdStrings.contains(_))
        listItemsFound.size must beEqualTo(3)
    }

  }


  "delete collection should remove collection id from shared items" in new CollectionSharingScope {
    ContentCollection.shareItems(organizationA,collectionB1ItemIds,collectionA1) match {
      case Left(error) => failure
      case Right(savedIds) =>
        // now delete collection a1 and make sure that collectionB1 items don't still have a refernce to it
        ContentCollection.delete(collectionA1)
        val oids = collectionB1ItemIds.map(i => i.id)
        val query = MongoDBObject("_id._id" -> MongoDBObject("$in" -> oids))
        val sharedCollfound = ItemServiceImpl.find(query).filter(item => {
          item.sharedInCollections.contains(collectionA1)
        })
        sharedCollfound.size must beEqualTo(0)

    }

  }


  "org can enable / disable a collection" in new CollectionSharingScope {
    val disableCollectionRequest =
      FakeRequest(GET, s"/api/v1/collections/$collectionB1/set-enabled-status/false?access_token=%s".format(accessTokenA))
    val disableCollectionResult = CollectionApi.setEnabledStatus(collectionB1, false)(disableCollectionRequest)
    assertResult(disableCollectionResult)
  }

  "find items/list items should only find items in 'enabled' collections for an org" in {
    pending
  }

  "only an owner of a collection can share that collection with another organization" in {
    pending
  }

  "json returned for content coll ref should include owner id, or isowner for current coll" in  {
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

  // enable collection b1 in org A
  Organization.setCollectionEnabledStatus(organizationA, collectionB1, true)

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