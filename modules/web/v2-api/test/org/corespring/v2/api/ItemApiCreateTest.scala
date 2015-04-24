package org.corespring.v2.api

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.models.item.index.ItemIndexSearchResult
import org.corespring.platform.core.services.item.{ItemIndexQuery, ItemIndexService, ItemService}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.test.PlaySingleton
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent._
import scalaz.{ Failure, Success, Validation }

class ItemApiCreateTest extends Specification with Mockito with MockFactory {

  /**
   * We should not need to run the app for a unit test.
   * However the way the app is tied up (global Dao Objects) - we need to boot a play application.
   */
  PlaySingleton.start()

  def FakeJsonRequest(json: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("", "", FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsJson(json))

  case class createApiScope(
    defaultCollectionId: ObjectId = ObjectId.get,
    insertFails: Boolean = false,
    canCreate: Validation[V2Error, Boolean] = Success(true)) extends Scope {
    lazy val api = new ItemApi {

      override def getSummaryData: (Item, Option[String]) => JsValue = transformItemToJson

      private def transformItemToJson(item: Item, detail: Option[String]): JsValue = {
        Json.toJson(item)
      }

      override def itemService: ItemService = {
        val m = mock[ItemService]
        val out = if (insertFails) None else Some(VersionedId(ObjectId.get))
        m.insert(any[Item]).returns(out)
        m
      }

      override def itemAuth: ItemAuth[OrgAndOpts] = {
        val m = mock[ItemAuth[OrgAndOpts]]
        m.canCreateInCollection(anyString)(any[OrgAndOpts]) returns canCreate
        m
      }

      override def itemIndexService = {
        val m = mock[ItemIndexService]
        m.reindex(any[VersionedId[ObjectId]]) returns future { Success("") }
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = Some(defaultCollectionId.toString)

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = canCreate.map(_ => mockOrgAndOpts())

      override def scoreService: ScoreService = {
        val m = mock[ScoreService]
        m
      }
    }
  }

  "V2 - ItemApi" should {

    "when calling create" should {

      "ignores the id in the request" in new createApiScope {
        val result = api.create()(FakeJsonRequest(Json.obj("id" -> "blah")))
        (contentAsJson(result) \ "id").as[String] !== "blah"
        (contentAsJson(result) \ "collectionId").as[String] === defaultCollectionId.toString
        status(result) === OK
      }

      "if there's no json in the body - it uses the default json" in new createApiScope {
        val result = api.create()(FakeJsonRequest(Json.obj()))
        (contentAsJson(result) \ "collectionId").as[String] === defaultCollectionId.toString
        (contentAsJson(result) \ "playerDefinition").as[JsObject] === api.defaultPlayerDefinition
        status(result) === OK
      }

      "if there's no player definition in the json body - it adds the default player definition" in new createApiScope {
        val customCollectionId = ObjectId.get
        val result = api.create()(FakeJsonRequest(Json.obj("collectionId" -> customCollectionId.toString)))
        (contentAsJson(result) \ "collectionId").as[String] === customCollectionId.toString
        (contentAsJson(result) \ "playerDefinition").as[JsObject] === api.defaultPlayerDefinition
        status(result) === OK
      }

      s"returns $OK - it ignores bad json, if it can't be parsed" in new createApiScope {
        val customCollectionId = ObjectId.get
        val result = api.create()(
          FakeRequest("", "", FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsText("bad")))
        status(result) === OK
      }

      s"returns $UNAUTHORIZED - if permission denied" in new createApiScope(
        canCreate = Failure(generalError("Nope", UNAUTHORIZED))) {
        val result = api.create()(FakeJsonRequest(Json.obj()))
        val e = canCreate.toEither.left.get
        status(result) === e.statusCode
        contentAsJson(result) === e.json
      }

      s"create - returns error with a bad save" in new createApiScope(
        insertFails = true) {
        val result = api.create()(FakeJsonRequest(Json.obj()))
        val e = errorSaving("Insert failed")
        status(result) === e.statusCode
        contentAsJson(result) === e.json
      }
    }

  }
}
