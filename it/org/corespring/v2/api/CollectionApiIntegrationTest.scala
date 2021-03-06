package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ CollectionHelper, ItemHelper, AccessTokenHelper, OrganizationHelper }
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessTokenAndItem }
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.json.ContentCollectionWrites
import org.corespring.v2.errors.Errors.{ propertyNotFoundInJson, propertyNotAllowedInJson }
import org.specs2.mutable.After
import org.specs2.specification.{ Fragments, Scope }
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.libs.json.Json._
import play.api.mvc.{ Call, Request, AnyContentAsJson }
import play.api.test.FakeRequest
import play.api.mvc.SimpleResult

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

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
      val coll = main.contentCollectionService.findOneById(collectionId).get
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
      main.contentCollectionService.findOneById(collectionId).get.name === "zowie"
    }

    "update the name and isPublic" in new update {
      val collection = main.contentCollectionService.findOneById(collectionId).get
      override lazy val json = Json.obj("name" -> "zowie", "isPublic" -> !collection.isPublic)
      override lazy val request = makeJsonRequest(call, json)
      status(result) === OK
      (contentAsJson(result) \ "name").asOpt[String] === Some("zowie")

      val updated = main.contentCollectionService.findOneById(collectionId).get
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
      main.contentCollectionService.findOneById(new ObjectId(newId)).get.name === "my-new-collection"
    }
  }

  "shareCollection" should {

    trait baseShare extends scope {
      val otherOrgId = OrganizationHelper.create("other org")
      logger.debug(s"share collectionId: $collectionId with: orgId: $otherOrgId")
      val call = Routes.shareCollection(collectionId, otherOrgId)
      def getShareResult(call: Call): Future[SimpleResult]
      lazy val shareResult = getShareResult(call)

      Await.result(shareResult, 2.seconds)

      val listCollectionsCall = Routes.list()

      logger.debug(s"call list for otherOrgId: $otherOrgId")
      val otherAccessToken = AccessTokenHelper.create(otherOrgId)
      val listRequest = FakeRequest(listCollectionsCall.method, mkUrl(listCollectionsCall.url, otherAccessToken))
      lazy val listCollectionsResult = route(listRequest).get
      lazy val json = contentAsJson(listCollectionsResult)
      lazy val ids = (json \\ "id").map(_.as[String])

      protected def getPermissionForCollectionId(id: ObjectId) = {

        logger.debug(s"function=getPermissionForCollectionId\n${prettyPrint(json)}")
        val info = json.as[Seq[JsValue]].find(j => (j \ "id").asOpt[String] == Some(id.toString))
        info.map(j => (j \ "permission").asOpt[String].getOrElse("?"))
      }
    }

    trait share extends baseShare {
      override def getShareResult(c: Call) = route(makeRequest(c)).get
    }

    "should return OK" in new share {
      status(shareResult) === OK
    }

    "list for other org should return OK" in new share {
      status(listCollectionsResult) === OK
    }

    "list for other org should contain the shared id" in new share {
      ids.contains(collectionId.toString) === true
    }

    s"""list the ${Permission.Read.name} permission for the newly shared collection""" in new share {
      Await.result(shareResult, 2.seconds)
      getPermissionForCollectionId(collectionId) must_== Some(Permission.Read.name)
    }

    "shareCollection" should {
      trait shareWithPermission extends baseShare {
        def permission: Permission
        override def getShareResult(call: Call): Future[SimpleResult] = {
          route(makeJsonRequest(call, Json.obj("permission" -> permission.name)))(writeableOf_AnyContentAsJson).get
        }
      }

      def assertShare(p: Permission): Fragments = {

        s"with ${p.name}" should {
          s"""return 200 when the client passes in {"permission":"${p.name}"}""" in new shareWithPermission {
            override def permission: Permission = p
            status(shareResult) must_== OK
          }

          //          s"""list the ${p.name} permission for the newly shared collection""" in new shareWithPermission {
          //            override def permission: Permission = p
          //            Await.result(shareResult, 2.seconds)
          //            getPermissionForCollectionId(collectionId) must_== Some(p.name)
          //          }
        }
      }

      //      assertShare(Permission.Clone)
      //      assertShare(Permission.Write)
      assertShare(Permission.Read)

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
