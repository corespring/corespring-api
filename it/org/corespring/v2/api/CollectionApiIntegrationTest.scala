package org.corespring.v2.api

import global.Global
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ CollectionHelper, ItemHelper, AccessTokenHelper, OrganizationHelper }
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessTokenAndItem }
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.json.ContentCollectionWrites
import org.corespring.v2.errors.Errors.{ propertyNotFoundInJson, propertyNotAllowedInJson }
import org.specs2.mutable.After
import org.specs2.specification.Scope
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.mvc.{ Request, AnyContentAsJson }
import play.api.test.FakeRequest

class CollectionApiIntegrationTest extends IntegrationSpecification {

  import org.corespring.v2.api.routes.{ CollectionApi => Routes }
  trait scope extends Scope with orgWithAccessTokenAndItem with TokenRequestBuilder with After {
    override def after = removeData()
  }

  "get" should {

    trait get extends scope {
      val call = Routes.getCollection(collectionId)
      val request = makeRequest(call)
      lazy val result = route(request).get
    }

    "return a contentCollection" in new get {
      status(result) === OK
      val coll = Global.main.contentCollectionService.findOneById(collectionId).get
      contentAsJson(result) === Json.toJson(coll)(ContentCollectionWrites)
    }
  }

  "updateCollection" should {

    trait update extends scope {
      val call = Routes.updateCollection(collectionId)
      def json: JsValue = Json.obj()
      def request: Request[AnyContentAsJson]
      lazy val result = {
        logger.debug(s"request :: $request, ${request.body}")
        route(request)(writeableOf_AnyContentAsJson).get
      }
    }

    "update the name only" in new update {
      override lazy val json = Json.obj("name" -> "zowie")
      override lazy val request = makeJsonRequest(call, json)
      status(result) === OK
      (contentAsJson(result) \ "name").asOpt[String] === Some("zowie")
      Global.main.contentCollectionService.findOneById(collectionId).get.name === "zowie"
    }

    "update the name and isPublic" in new update {
      val collection = Global.main.contentCollectionService.findOneById(collectionId).get
      override lazy val json = Json.obj("name" -> "zowie", "isPublic" -> !collection.isPublic)
      override lazy val request = makeJsonRequest(call, json)
      status(result) === OK
      (contentAsJson(result) \ "name").asOpt[String] === Some("zowie")

      val updated = global.Global.main.contentCollectionService.findOneById(collectionId).get
      updated.name === "zowie"
      updated.isPublic === !collection.isPublic
    }
  }

  "createCollection" should {

    trait create extends scope {
      val call = Routes.createCollection()
      def request: Request[AnyContentAsJson]
      lazy val result = route(request)(writeableOf_AnyContentAsJson).get
    }

    "fail if there's an id in the request json" in new create {
      val json = Json.obj("id" -> ObjectId.get.toString)
      override lazy val request = makeJsonRequest(call, json)
      status(result) === BAD_REQUEST
      contentAsJson(result) === propertyNotAllowedInJson("id", json).json
    }

    "fail if there is no json" in new scope {
      val call = Routes.createCollection()
      val request = makeRequest(call)
      val result = route(request).get
      status(result) === 400
    }

    "fail if theres no 'name' in the request json" in new create {
      val json = Json.obj("my-name" -> "my-new-collection")
      override lazy val request = makeJsonRequest(call, json)
      status(result) === BAD_REQUEST
      contentAsJson(result) === propertyNotFoundInJson("name").json
    }

    "create the collection" in new create {
      val json = Json.obj("name" -> "my-new-collection")
      override lazy val request = makeJsonRequest(call, json)
      status(result) === CREATED
      val newId = (contentAsJson(result) \ "id").asOpt[String].get
      Global.main.contentCollectionService.findOneById(new ObjectId(newId)).get.name === "my-new-collection"
    }
  }

  "shareCollection" should {

    trait share extends scope {
      val otherOrgId = OrganizationHelper.create("other org")
      val call = Routes.shareCollection(collectionId, otherOrgId)
      val result = route(makeRequest(call)).get
      val listCollectionsCall = Routes.list()
      val otherAccessToken = AccessTokenHelper.create(otherOrgId)
      val request = FakeRequest(listCollectionsCall.method, mkUrl(listCollectionsCall.url, otherAccessToken))
      val listCollectionsResult = route(makeRequest(listCollectionsCall)).get
      val json = contentAsJson(listCollectionsResult)
      val ids = (json \\ "id").map(_.as[String])
    }

    "should return OK" in new share {
      status(result) === OK
    }

    "list for other org should return OK" in new share {
      status(listCollectionsResult) === OK
    }

    "list for other org should contain the shared id" in new share {
      ids.contains(collectionId.toString) === true
    }
  }

  "setEnabledStatus" should {
    "work" in pending
  }

  "deleteCollection" should {
    "work" in pending
  }

  "list" should {

    trait list extends scope {

      val items = (1 to 20).map { i =>

        val tmpCollectionId = CollectionHelper.create(orgId, s"collection-$i")
        val item = Item(collectionId = tmpCollectionId.toString, taskInfo = Some(TaskInfo(title = Some(s"title-$i"))))
        val vid = ItemHelper.create(tmpCollectionId, item)
        item.copy(id = vid)
      }
      def skip: Int = 0
      def limit: Int = 0
      lazy val listCollectionsCall = Routes.list(sk = skip, l = limit)
      lazy val request = makeRequest(listCollectionsCall)
      lazy val listCollectionsResult = route(request).get
      lazy val json = contentAsJson(listCollectionsResult)
      logger.debug(s"json=$json")
      lazy val ids = (json \\ "id").map(_.as[String])
    }

    "supports limit" in new list {
      override lazy val limit = 5
      ids.length === 5
    }

    "supports skip" in new list {
      override lazy val skip = 5
      (json.as[JsArray].value(0) \ "name").asOpt[String] === Some("collection-5")
    }

    "supports skip and limit" in new list {
      override lazy val skip = 10
      override lazy val limit = 4
      (json.as[JsArray].value(0) \ "name").asOpt[String] === Some("collection-10")
      ids.length === 4
    }
  }

}
