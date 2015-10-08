package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessTokenAndItem }
import org.corespring.models.json.ContentCollectionWrites
import org.corespring.v2.errors.Errors.{ propertyNotFoundInJson, propertyNotAllowedInJson }
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Request, AnyContent, AnyContentAsJson }

class CollectionApiIntegrationTest extends IntegrationSpecification {

  import org.corespring.v2.api.routes.{ CollectionApi => Routes }
  trait scope extends Scope with orgWithAccessTokenAndItem with TokenRequestBuilder

  "get" should {

    trait get extends scope {
      val call = Routes.getCollection(collectionId)
      val request = makeRequest(call)
      lazy val result = route(request).get
    }

    "return a contentCollection" in new get {
      status(result) === OK
      val coll = bootstrap.Main.contentCollectionService.findOneById(collectionId).get
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
      bootstrap.Main.contentCollectionService.findOneById(collectionId).get.name === "zowie"
    }

    "update the name and isPublic" in new update {
      val collection = bootstrap.Main.contentCollectionService.findOneById(collectionId).get
      override lazy val json = Json.obj("name" -> "zowie", "isPublic" -> !collection.isPublic)
      override lazy val request = makeJsonRequest(call, json)
      status(result) === OK
      (contentAsJson(result) \ "name").asOpt[String] === Some("zowie")

      val updated = bootstrap.Main.contentCollectionService.findOneById(collectionId).get
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
      bootstrap.Main.contentCollectionService.findOneById(new ObjectId(newId)).get.name === "my-new-collection"
    }
  }

  "shareCollection" should {
    "work" in pending
  }

  "setEnabledStatus" should {
    "work" in pending
  }

  "deleteCollection" should {
    "work" in pending
  }

}
