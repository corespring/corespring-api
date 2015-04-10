package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.services.item.{ItemIndexService, ItemService}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class ItemApiGetTest extends Specification with Mockito with MockFactory {

  /**
   * We should not need to run the app for a unit test.
   * However the way the app is tied up (global Dao Objects) - we need to boot a play application.
   */
  PlaySingleton.start()

  def FakeJsonRequest(json: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("", "", FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsJson(json))

  case class getApiScope(
    defaultCollectionId: ObjectId = ObjectId.get,
    loadForRead: Validation[V2Error, Item] = Failure(notReady)) extends Scope {

    lazy val api = new ItemApi {

      override def scoreService: ScoreService = {
        val m = mock[ScoreService]
        m
      }

      override def transform: (Item, Option[String]) => JsValue = transformItemToJson

      def transformItemToJson(item: Item, detail: Option[String] = None): JsValue = {
        Json.toJson(item)
      }

      override def itemService: ItemService = {
        val m = mock[ItemService]
        m
      }

      override def itemAuth: ItemAuth[OrgAndOpts] = {
        val m = mock[ItemAuth[OrgAndOpts]]
        m.loadForRead(anyString)(any[OrgAndOpts]) returns loadForRead
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = Some(defaultCollectionId.toString)

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = loadForRead.map(_ => mockOrgAndOpts())

      override def itemIndexService: ItemIndexService = ???
    }
  }

  "V2 - ItemApi" should {

    "when calling get" should {

      s"returns $UNAUTHORIZED - if permission denied" in new getApiScope(
        loadForRead = Failure(generalError("Nope", UNAUTHORIZED))) {

        val id = VersionedId(ObjectId.get)
        val result = api.get(id.toString())(FakeJsonRequest(Json.obj()))
        val e = loadForRead.toEither.left.get
        status(result) === e.statusCode
        contentAsJson(result) === e.json
      }

      "returns item" in new getApiScope(
        loadForRead = Success(new Item(id = VersionedId(ObjectId.get)))) {

        val expectedItem = loadForRead.toEither.right.get

        val result = api.get(expectedItem.id.toString)(FakeJsonRequest(Json.obj()))
        import scala.language.reflectiveCalls
        contentAsJson(result) === api.transformItemToJson(expectedItem)
      }

    }

  }
}
