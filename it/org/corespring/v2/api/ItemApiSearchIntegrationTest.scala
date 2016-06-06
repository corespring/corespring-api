package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ CollectionHelper, ItemHelper }
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessToken }
import org.corespring.models.item.{ Item, TaskInfo }
import org.specs2.mutable.After
import org.specs2.specification.Scope
import play.api.libs.json.JsArray

class ItemApiSearchIntegrationTest extends IntegrationSpecification {

  trait scope extends Scope with orgWithAccessToken with TokenRequestBuilder with After {

    val collectionId = CollectionHelper.create(orgId)
    val item = Item(collectionId = collectionId.toString, taskInfo = Some(TaskInfo(title = Some("Item title"))))
    val itemId = ItemHelper.create(collectionId, item)

    override def after = removeData()

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

    trait withJsonResult extends searchByCollectionId {
      val result = route(request).get
      val jsArray: JsArray = contentAsJson(result).as[JsArray]
      logger.debug(s"jsArray: $jsArray")
    }

    "return items as an Array" in new withJsonResult {
      jsArray.value.size === 1
    }

    "returns the the items' title in the array" in new withJsonResult {
      jsArray.value.map(json => (json \ "title").as[String]) === Seq("Item title")
    }

    "returns the the items' collectionId in the array" in new withJsonResult {
      jsArray.value.map(json => (json \ "collectionId").as[String]) === Seq(collectionId.toString)
    }
  }

}
