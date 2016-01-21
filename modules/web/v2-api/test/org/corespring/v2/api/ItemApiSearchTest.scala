package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.itemSearch.{ ItemIndexHit, ItemIndexQuery, ItemIndexSearchResult }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models._
import org.corespring.v2.errors.Errors.invalidToken
import org.corespring.v2.errors.V2Error
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.duration._
import scalaz._

class ItemApiSearchTest extends ItemApiSpec {

  private def waitFor[A](f: Future[A]): A = {
    Await.result(f, 2.seconds)
  }

  case class searchApiScope(override val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts()),
    searchResult: ItemIndexSearchResult = ItemIndexSearchResult(0, Seq.empty)) extends ItemApiScope {
    import ExecutionContext.Implicits.global
    itemIndexService.search(any[ItemIndexQuery]) returns future { Success(searchResult) }
    itemIndexService.reindex(any[VersionedId[ObjectId]]) returns future { Success("") }
  }

  "search" should {

    implicit val ItemIndexSearchResultFormat = ItemIndexSearchResult.Format
    val allowableCollections = (1 to 5).map(i => new ObjectId())
    val restrictedCollection = new ObjectId()

    "call itemIndexService#search" in new searchApiScope {
      val result = api.search(Some("{}"))(FakeJsonRequest(Json.obj()))
      waitFor(result)
      there was one(itemIndexService).search(any[ItemIndexQuery])
    }

    "with empty collections" should {

      "call itemIndexService#search with allowed collections" in
        new searchApiScope(orgAndOpts = Success(mockOrgAndOpts(collections = allowableCollections))) {
          val query = Json.obj("collections" -> Seq()).toString
          val result = api.search(Some(query))(FakeJsonRequest(Json.obj()))
          waitFor(result)
          there was one(itemIndexService).search(ItemIndexQuery(collections = allowableCollections.map(_.toString)))
        }
    }

    "with unallowed collections" should {

      "call itemIndexService#search with allowed collections" in
        new searchApiScope(orgAndOpts = Success(mockOrgAndOpts(collections = allowableCollections))) {
          val query = Json.obj("collections" -> Seq(restrictedCollection.toString)).toString
          val result = api.search(Some(query))(FakeJsonRequest(Json.obj()))
          waitFor(result)
          there was one(itemIndexService).search(ItemIndexQuery(collections = allowableCollections.map(_.toString)))
        }

    }

    "with allowed and unallowed collections" should {

      "call itemIndexService#search with only allowed collections" in
        new searchApiScope(
          orgAndOpts = Success(mockOrgAndOpts(collections = allowableCollections))) {
          val query = Json.obj("collections" -> Seq(allowableCollections.head.toString, restrictedCollection.toString)).toString
          val result = api.search(Some(query))(FakeJsonRequest(Json.obj()))
          waitFor(result)
          there was one(itemIndexService).search(ItemIndexQuery(collections = Seq(allowableCollections.head.toString)))
        }

    }

    "return results as JSON" in new searchApiScope {
      val result = api.search(Some("{}"))(FakeJsonRequest(Json.obj()))
      contentAsJson(result) must_== Json.toJson(searchResult)
    }

    "without proper authentication" should {

      "return unauthorized" in new searchApiScope(orgAndOpts = Failure(invalidToken(FakeJsonRequest()))) {
        val result = api.search(Some("{}"))(FakeJsonRequest(Json.obj()))
        status(result) must_== UNAUTHORIZED
      }
    }

    "with bad json" should {

      "return bad request" in new searchApiScope {
        val result = api.search(Some("this is not json."))(FakeJsonRequest(Json.obj()))
        status(result) must_== BAD_REQUEST
      }
    }
  }

  "searchByCollectionId" should {
    "returns an error if it can't parse the query string" in new searchApiScope {
      val result = api.searchByCollectionId(ObjectId.get(), Some("this is not json"))(FakeJsonRequest(Json.obj()))
      status(result) must_== BAD_REQUEST
    }

    "calls itemIndexService.search with the query" in new searchApiScope {
      val query = """{"offset":  4, "count" : 2, "text" : "hi"}"""
      val result = api.searchByCollectionId(collectionId, Some(query))(FakeJsonRequest(Json.obj()))
      status(result) must_== OK
      there was one(itemIndexService).search(ItemIndexQuery(offset = 4, count = 2, text = Some("hi"), collections = Seq(collectionId.toString)))
    }

    def hit(title: String) = ItemIndexHit("id",
      None,
      None,
      false,
      Map.empty,
      None,
      Seq.empty,
      Some(title),
      None,
      None,
      0,
      Seq.empty)

    "returns a json array" in new searchApiScope(
      searchResult =
        ItemIndexSearchResult(total = 1, hits = Seq(hit("one")))) {
      val result = api.searchByCollectionId(collectionId, None)(FakeJsonRequest(Json.obj()))
      implicit val f = ItemIndexHit.Format
      contentAsJson(result) must_== Json.arr(Json.toJson(hit("one")))
    }
  }

}
