package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.itemSearch.{ ItemIndexQuery, ItemIndexSearchResult }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.models._
import org.corespring.v2.errors.Errors.invalidToken
import org.corespring.v2.errors.V2Error
import play.api.libs.json._
import play.api.test.Helpers._

import scala.concurrent._
import scalaz._

class ItemApiSearchTest extends ItemApiSpec {

  case class searchApiScope(override val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts()),
    searchResult: ItemIndexSearchResult = ItemIndexSearchResult(0, Seq.empty)) extends ItemApiScope {
    import ExecutionContext.Implicits.global
    mockItemIndexService.search(any[ItemIndexQuery]) returns future { Success(searchResult) }
    mockItemIndexService.reindex(any[VersionedId[ObjectId]]) returns future { Success("") }
  }

  "search" should {

    implicit val ItemIndexSearchResultFormat = ItemIndexSearchResult.Format
    val allowableCollections = (1 to 5).map(i => new ObjectId())
    val unallowedCollection = new ObjectId()

    "call itemIndexService#search" in new searchApiScope {
      val result = api.search(Some("{}"))(FakeJsonRequest(Json.obj()))
      there was one(mockItemIndexService).search(any[ItemIndexQuery])
    }

    "with empty collections" should {

      "call itemIndexService#search with allowed collections" in
        new searchApiScope(orgAndOpts = Success(mockOrgAndOpts(collections = allowableCollections))) {
          val query = Json.obj("collections" -> Seq()).toString
          val result = api.search(Some(query))(FakeJsonRequest(Json.obj()))
          there was one(mockItemIndexService).search(ItemIndexQuery(collections = allowableCollections.map(_.toString)))
        }
    }

    "with unallowed collections" should {

      "call itemIndexService#search with allowed collections" in
        new searchApiScope(orgAndOpts = Success(mockOrgAndOpts(collections = allowableCollections))) {
          val query = Json.obj("collections" -> Seq(unallowedCollection.toString)).toString
          val result = api.search(Some(query))(FakeJsonRequest(Json.obj()))
          there was one(mockItemIndexService).search(ItemIndexQuery(collections = allowableCollections.map(_.toString)))
        }

    }

    "with allowed and unallowed collections" should {

      "call itemIndexService#search with only allowed collections" in
        new searchApiScope(orgAndOpts = Success(mockOrgAndOpts(collections = allowableCollections))) {
          val query = Json.obj("collections" -> Seq(allowableCollections.head.toString, unallowedCollection.toString)).toString
          val result = api.search(Some(query))(FakeJsonRequest(Json.obj()))
          there was one(mockItemIndexService).search(ItemIndexQuery(collections = Seq(allowableCollections.head.toString)))
        }

    }

    "return results as JSON" in new searchApiScope {
      val result = api.search(Some("{}"))(FakeJsonRequest(Json.obj()))
      contentAsJson(result) === Json.toJson(searchResult)
    }

    "without proper authentication" should {

      "return unauthorized" in new searchApiScope(orgAndOpts = Failure(invalidToken(FakeJsonRequest()))) {
        val result = api.search(Some("{}"))(FakeJsonRequest(Json.obj()))
        status(result) === (UNAUTHORIZED)
      }

    }

    "with bad json" should {

      "return bad request" in new searchApiScope {
        val result = api.search(Some("this is not json."))(FakeJsonRequest(Json.obj()))
        status(result) === (BAD_REQUEST)
      }

    }
  }

}
