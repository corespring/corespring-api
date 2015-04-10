package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.index.ItemIndexSearchResult
import org.corespring.platform.core.services.item._
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.models._
import org.corespring.v2.errors.Errors.invalidToken
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent._
import scalaz._

class ItemApiSearchTest extends Specification with Mockito with MockFactory {

  /**
   * We should not need to run the app for a unit test.
   * However the way the app is tied up (global Dao Objects) - we need to boot a play application.
   */
  PlaySingleton.start()

  def FakeJsonRequest(json: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("", "",
    FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsJson(json))

  case class searchApiScope(orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts()),
                            searchResult: ItemIndexSearchResult = ItemIndexSearchResult(0, Seq.empty)) extends Scope {
    import ExecutionContext.Implicits.global

    val indexService = mock[ItemIndexService]
    indexService.search(any[ItemIndexQuery]) returns future { Success(searchResult) }

    lazy val api = new ItemApi {
      implicit def ec: ExecutionContext = ExecutionContext.Implicits.global
      def getOrgAndOptions(request: RequestHeader) = orgAndOpts
      def itemIndexService = indexService
      def defaultCollection(implicit identity: OrgAndOpts) = ???
      def transform = ???
      def itemService = ???
      def itemAuth = ???
      def scoreService = ???
    }
  }

  "search" should {

    implicit val ItemIndexSearchResultFormat = ItemIndexSearchResult.Format
    val allowableCollections = (1 to 5).map(i => new ObjectId())
    val unallowedCollection = new ObjectId()

    "call itemIndexService#search" in new searchApiScope {
      val result = api.search(Some("{}"))(FakeJsonRequest(Json.obj()))
      there was one(indexService).search(any[ItemIndexQuery])
    }

    "with empty collections" should {

      "call itemIndexService#search with allowed collections" in
        new searchApiScope(orgAndOpts = Success(mockOrgAndOpts(collections = allowableCollections))) {
          val query = Json.obj("collections" -> Seq()).toString
          val result = api.search(Some(query))(FakeJsonRequest(Json.obj()))
          there was one(indexService).search(ItemIndexQuery(collections = allowableCollections.map(_.toString)))
        }
    }

    "with unallowed collections" should {

      "call itemIndexService#search with allowed collections" in
        new searchApiScope(orgAndOpts = Success(mockOrgAndOpts(collections = allowableCollections))) {
          val query = Json.obj("collections" -> Seq(unallowedCollection.toString)).toString
          val result = api.search(Some(query))(FakeJsonRequest(Json.obj()))
          there was one(indexService).search(ItemIndexQuery(collections = allowableCollections.map(_.toString)))
        }

    }

    "with allowed and unallowed collections" should {

      "call itemIndexService#search with only allowed collections" in
        new searchApiScope(orgAndOpts = Success(mockOrgAndOpts(collections = allowableCollections))) {
          val query = Json.obj("collections" -> Seq(allowableCollections.head.toString, unallowedCollection.toString)).toString
          val result = api.search(Some(query))(FakeJsonRequest(Json.obj()))
          there was one(indexService).search(ItemIndexQuery(collections = Seq(allowableCollections.head.toString)))
        }

    }

    "return results as JSON" in new searchApiScope {
      val result = api.search(Some("{}"))(FakeJsonRequest(Json.obj()))
      contentAsJson(result) === Json.toJson(searchResult)
    }

    "without proper authentication" should {

      "return unauthorized" in new searchApiScope(orgAndOpts = Failure(invalidToken(FakeJsonRequest(Json.obj())))) {
        val result = api.search(Some("{}"))(FakeJsonRequest(Json.obj()))
        status(result) must be equalTo (UNAUTHORIZED)
      }

    }

    "with bad json" should {

      "return bad request" in new searchApiScope {
        val result = api.search(Some("this is not json."))(FakeJsonRequest(Json.obj()))
        status(result) must be equalTo (BAD_REQUEST)
      }

    }
  }

}
