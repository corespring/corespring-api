package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ CollectionHelper, ItemHelper }
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessToken }
import org.corespring.models.item.{ Item, TaskInfo }
import org.specs2.mutable.After
import org.specs2.specification.Scope
import play.api.libs.json.{ JsArray, JsObject, JsValue }
import play.api.libs.json.Json._

class V2ItemApiSearchIntegrationTest extends IntegrationSpecification {

  trait scope extends Scope with orgWithAccessToken with TokenRequestBuilder with After {

    val collectionId = CollectionHelper.create(orgId)
    val item = Item(collectionId = collectionId.toString, taskInfo = Some(TaskInfo(title = Some("Item title"))))
    val itemId = ItemHelper.create(collectionId, item)
    val itemService = global.Global.main.itemService
    val Routes = org.corespring.v2.api.routes.ItemApi
    override def after = removeData()

  }

  "searchByCollectionId" should {

    trait searchByCollectionId extends scope {
      def query: Option[String] = None
      val call = Routes.searchByCollectionId(collectionId, query)
      val request = makeRequest(call)
    }

    s"return $OK" in new searchByCollectionId {
      val result = route(request).get
      status(result) must_== OK
    }

    trait withJsonResult extends searchByCollectionId {
      val result = route(request).get
      val jsArray: JsArray = contentAsJson(result).as[JsArray]
      logger.debug(s"jsArray: $jsArray")
    }

    "return items as an Array" in new withJsonResult {
      jsArray.value.size must_== 1
    }

    "returns the the items' title in the array" in new withJsonResult {
      jsArray.value.map(json => (json \ "title").as[String]) must_== Seq("Item title")
    }

    "returns the the items' collectionId in the array" in new withJsonResult {
      jsArray.value.map(json => (json \ "collectionId").as[String]) must_== Seq(collectionId.toString)
    }
  }

  "search" should {

    trait searchBase extends scope {
      def query: Option[JsObject] = None
      itemService.publish(itemId)
      val unpublishedItem = itemService.getOrCreateUnpublishedVersion(itemId).get
      val call = Routes.search(query.map(_.toString))
      val request = makeRequest(call)
      lazy val result = route(request).get
    }

    class search(published: Option[Boolean], latest: String = "true") extends searchBase {
      override lazy val query: Option[JsObject] = {
        val p = published.map { p => obj("published" -> p) }.getOrElse(obj())
        Some(obj("latest" -> latest) ++ p)
      }
    }

    trait defaultSearch extends searchBase

    "with no query" should {

      s"return status $OK" in new defaultSearch {
        status(result) must_== OK
      }

      s"return the latest entry" in new defaultSearch {
        val json = contentAsJson(result)
        val head = (json \ "hits").as[Seq[JsValue]].head
        (head \ "id").as[String] must_== unpublishedItem.id.toString
        (head \ "published").as[Boolean] must_== false
      }
    }

    "with published:true and latest:skip" should {
      s"return the penultimate published item" in new search(published = Some(true), latest = "skip") {
        val json = contentAsJson(result)
        val head = (json \ "hits").as[Seq[JsValue]].head
        (head \ "id").as[String] must_== item.id.toString
        (head \ "published").as[Boolean] must_== true
      }
    }

    "with published:true and latest:yes" should {
      s"return an empty list" in new search(published = Some(true), latest = "yes") {
        val json = contentAsJson(result)
        val head = (json \ "hits").as[Seq[JsValue]].length must_== 0
      }
    }

    "with published:false and latest:yes" should {
      s"return the most recent unpublished item" in new search(published = Some(false), latest = "yes") {
        val json = contentAsJson(result)
        val head = (json \ "hits").as[Seq[JsValue]].head
        (head \ "id").as[String] must_== unpublishedItem.id.toString
        (head \ "published").as[Boolean] must_== false
      }
    }

  }

}
