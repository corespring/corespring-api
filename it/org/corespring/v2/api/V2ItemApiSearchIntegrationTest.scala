package org.corespring.v2.api

import javax.transaction.Transaction

import bootstrap.Main
import org.bson.types.ObjectId
import org.corespring.it.contexts.OrgWithAccessToken
import org.corespring.it.{ IntegrationSpecification, ItemIndexCleaner }
import org.corespring.it.helpers.{ CollectionHelper, ItemHelper }
import org.corespring.it.scopes.{ TokenRequest, TokenRequestBuilder, orgWithAccessToken }
import org.corespring.models.item.{ Item, TaskInfo }
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.execute.{ AsResult, Result, Success }
import org.specs2.mutable.{ After, Before }
import org.specs2.specification.{ Fixture, Scope }
import play.api.libs.json.{ JsArray, JsObject, JsValue }
import play.api.libs.json.Json._
import play.api.mvc.AnyContentAsEmpty

class V2ItemApiSearchIntegrationTest extends IntegrationSpecification {

  val Routes = org.corespring.v2.api.routes.ItemApi

  trait scope extends Scope with orgWithAccessToken with TokenRequestBuilder with After {

    val collectionId = CollectionHelper.create(orgId)
    val item = Item(collectionId = collectionId.toString, taskInfo = Some(TaskInfo(title = Some("Item title"))))
    val itemId = ItemHelper.create(collectionId, item)
    val itemService = global.Global.main.itemService
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

    class search(published: Option[Boolean], mode: String) extends searchBase {
      override lazy val query: Option[JsObject] = {
        val p = published.map { p => obj("published" -> p) }.getOrElse(obj())
        Some(obj("mode" -> mode) ++ p)
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

    "with published:true and mode:latestPublished" should {
      s"return the penultimate published item" in new search(published = Some(true), mode = "latestPublished") {
        val json = contentAsJson(result)
        println(prettyPrint(json))
        val head = (json \ "hits").as[Seq[JsValue]].head
        (head \ "id").as[String] must_== item.id.toString
        (head \ "published").as[Boolean] must_== true
      }
    }

    "with published:true and mode:latest" should {
      s"return an empty list" in new search(published = Some(true), mode = "latest") {
        val json = contentAsJson(result)
        val head = (json \ "hits").as[Seq[JsValue]].length must_== 0
      }
    }

    "with published:false and mode:latest" should {
      s"return the most recent unpublished item" in new search(published = Some(false), mode = "latest") {
        val json = contentAsJson(result)
        val head = (json \ "hits").as[Seq[JsValue]].head
        (head \ "id").as[String] must_== unpublishedItem.id.toString
        (head \ "published").as[Boolean] must_== false
      }
    }
  }

  "search - large dataset" should {

    def largeSet(publishOdd: Boolean) = new Fixture[OrgWithAccessToken] with ItemIndexCleaner {
      def apply[R: AsResult](f: OrgWithAccessToken => R) = {

        cleanIndex()
        removeData()

        val ctx: OrgWithAccessToken = OrgWithAccessToken.apply
        val collectionId = CollectionHelper.create(ctx.org.id)
        val itemService = global.Global.main.itemService

        def addItem(index: Int) = {
          val item = Item(collectionId = collectionId.toString, taskInfo = Some(TaskInfo(title = Some(s"Item index $index"))))
          val itemId = ItemHelper.create(collectionId, item)

          //all even items will be latest/published - odd ones only latest
          if (index % 2 == 0) {
            itemService.publish(itemId)
            itemService.getOrCreateUnpublishedVersion(itemId)
          } else {
            if (publishOdd) {
              itemService.publish(itemId)
            }
          }
          index -> itemId
        }

        (0 to 49).map(addItem)
        val r = f(ctx)
        cleanIndex()
        removeData()
        AsResult(r)
      }
    }

    "returns half the number of items when mode:latestPublished" in largeSet(false) { ctx =>
      lazy val call = Routes.search(Some("""{"mode": "latestPublished"}"""))
      lazy val request = ctx.tokenRequestBuilder.makeRequest(call, AnyContentAsEmpty)
      lazy val result = route(request).get
      lazy val json = contentAsJson(result)
      (json \ "total").as[Int] must_== 25
    }

    "returns all the items when mode:latestPublished and odd items are also published" in largeSet(publishOdd = true) { ctx =>
      lazy val call = Routes.search(Some("""{"mode" : "latestPublished"}"""))
      lazy val request = ctx.tokenRequestBuilder.makeRequest(call, AnyContentAsEmpty)
      lazy val result = route(request).get
      lazy val json = contentAsJson(result)
      (json \ "total").as[Int] must_== 50
    }

    "returns all the items when mode:latest" in largeSet(publishOdd = false) { ctx =>
      lazy val call = Routes.search(Some("""{}"""))
      lazy val request = ctx.tokenRequestBuilder.makeRequest(call, AnyContentAsEmpty)
      lazy val result = route(request).get
      lazy val json = contentAsJson(result)
      (json \ "total").as[Int] must_== 50
    }

    "returns half the items when mode:latest and published:true" in largeSet(publishOdd = true) { ctx =>
      lazy val call = Routes.search(Some("""{"published" : true}"""))
      lazy val request = ctx.tokenRequestBuilder.makeRequest(call, AnyContentAsEmpty)
      lazy val result = route(request).get
      lazy val json = contentAsJson(result)
      (json \ "total").as[Int] must_== 25
    }

  }
}
