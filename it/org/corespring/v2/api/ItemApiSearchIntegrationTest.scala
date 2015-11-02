package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ CollectionHelper, ItemHelper }
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessToken }
import org.corespring.models.item.{ TaskInfo, Item }
import org.specs2.specification.Scope
import play.api.libs.json.JsArray

class ItemApiSearchIntegrationTest extends IntegrationSpecification {

  trait scope extends Scope with orgWithAccessToken with TokenRequestBuilder {

    val collectionId = CollectionHelper.create(orgId)
    val item = Item(collectionId = collectionId.toString, taskInfo = Some(TaskInfo(title = Some("Item title"))))
    val itemId = ItemHelper.create(collectionId, item)
  }

  "searchByCollectionId" should {

    trait searchByCollectionId extends scope {
      def query: Option[String] = None
      val call = org.corespring.v2.api.routes.ItemApi.searchByCollectionId(collectionId, query)
      val request = makeRequest(call)
    }

    s"return $OK" in new searchByCollectionId {
      val result = route(request).get
      status(result) === OK
    }

    "return items as an Array" in new searchByCollectionId {
      val result = route(request).get
      val jsArray: JsArray = contentAsJson(result).as[JsArray]
      jsArray.value.size === 1
    }

    "returns the first item's title in the Array" in new searchByCollectionId {
      val result = route(request).get
      val jsArray: JsArray = contentAsJson(result).as[JsArray]
      (jsArray.value(0) \ "title").asOpt[String] === Some("Item title")
    }
  }
}
