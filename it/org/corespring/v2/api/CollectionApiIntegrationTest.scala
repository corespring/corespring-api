package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessTokenAndItem }
import org.corespring.models.json.ContentCollectionWrites
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
  }
}
