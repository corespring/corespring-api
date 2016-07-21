package web.controllers

import org.bson.types.ObjectId
import org.corespring.itemSearch.{ ItemIndexHit, ItemIndexQuery, ItemIndexSearchResult, ItemIndexService }
import org.corespring.models.auth.{ ApiClient, Permission }
import org.corespring.services.OrgCollectionService
import org.corespring.v2.actions.{ OrgAndApiClientRequest, OrgRequest, V2Actions, V2ActionsFactory }
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import web.models.WebExecutionContext

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Success, Validation }

class ItemSearchTest extends Specification with Mockito with MockFactory {

  import ExecutionContext.Implicits.global

  def hit(collectionId: ObjectId) =
    ItemIndexHit(
      "hit",
      Some(collectionId.toString),
      None,
      false,
      Map.empty,
      None,
      Seq.empty,
      None,
      None,
      None,
      0,
      Seq.empty)

  val orgAndOpts = mockOrgAndOpts(AuthMode.UserSession)

  trait scope extends Scope {

    lazy val hits: Seq[ItemIndexHit] = Seq.empty

    lazy val permissions: Seq[(ObjectId, Option[Permission])] = Seq.empty

    lazy val searchService = {
      val m = mock[ItemIndexService]
      m.search(any[ItemIndexQuery], any[Option[String]]) returns {
        Future { Success(ItemIndexSearchResult(hits.size, hits)) }
      }
      m
    }

    lazy val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m.getPermissions(any[ObjectId], any[ObjectId]) returns {
        Future(permissions)
      }
      m
    }

    lazy val webExecutionContext = WebExecutionContext(ExecutionContext.Implicits.global)

    lazy val actions = V2ActionsFactory.apply

    val controller = new ItemSearch(
      actions,
      searchService,
      orgCollectionService,
      webExecutionContext)
  }

  "search" should {
    "return an empty result set if there are no collections in the query" in new scope {
      lazy val collectionId = ObjectId.get
      override lazy val hits = Seq.empty
      override lazy val permissions = Seq(collectionId -> Some(Permission.Write))
      val result = controller.search(None)(FakeRequest())
      status(result) must_== OK
      val json = contentAsJson(result)
      (json \ "total").asOpt[Int] must_== Some(0)
    }

    "return 1 result with Write" in new scope {
      lazy val collectionId = ObjectId.get
      override lazy val hits = Seq(hit(collectionId))
      override lazy val permissions = Seq(collectionId -> Some(Permission.Write))
      val result = controller.search(None)(FakeRequest())
      val json = contentAsJson(result)
      (json \ "hits").as[Seq[JsValue]] must_== Seq(
        ItemInfo(hit(collectionId), Some(Permission.Write)).json)
    }

    "return 2 results from the two collections with Write and Clone" in new scope {
      lazy val collectionId = ObjectId.get
      lazy val collectionIdTwo = ObjectId.get
      override lazy val hits = Seq(hit(collectionId), hit(collectionIdTwo))
      override lazy val permissions = Seq(collectionId -> Some(Permission.Write), collectionIdTwo -> Some(Permission.Clone))
      val result = controller.search(None)(FakeRequest())
      val json = contentAsJson(result)
      (json \ "hits").as[Seq[JsValue]] must_== Seq(
        ItemInfo(hit(collectionId), Some(Permission.Write)).json,
        ItemInfo(hit(collectionIdTwo), Some(Permission.Clone)).json)
    }

    "return 2 results from the same collection with Write" in new scope {
      lazy val collectionId = ObjectId.get
      override lazy val hits = Seq(hit(collectionId), hit(collectionId))
      override lazy val permissions = Seq(collectionId -> Some(Permission.Write))
      val result = controller.search(None)(FakeRequest())
      val json = contentAsJson(result)
      (json \ "hits").as[Seq[JsValue]] must_== Seq(
        ItemInfo(hit(collectionId), Some(Permission.Write)).json,
        ItemInfo(hit(collectionId), Some(Permission.Write)).json)
    }
  }
}
