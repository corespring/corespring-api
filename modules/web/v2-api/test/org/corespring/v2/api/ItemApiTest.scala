package org.corespring.v2.api

import org.corespring.v2.auth.models.{ AuthMode, PlayerAccessSettings, OrgAndOpts }

import scala.concurrent.ExecutionContext

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.errors.V2Error
import org.corespring.v2.errors.Errors.{ errorSaving, generalError, notReady }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._
import play.api.test.{ FakeHeaders, FakeRequest }
import play.api.test.Helpers._
import scalaz.{ Failure, Success, Validation }

class ItemApiTest extends Specification with Mockito {

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

      override def transform: (Item, Option[String]) => JsValue = transformItemToJson

      def transformItemToJson(item: Item, detail: Option[String]): JsValue = {
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

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = Some(defaultCollectionId.toString)

      override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = canCreate.map(_ => OrgAndOpts(ObjectId.get, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken))

    }
  }

  case class getApiScope(
    defaultCollectionId: ObjectId = ObjectId.get,
    loadForRead: Validation[V2Error, Item] = Failure(notReady)) extends Scope {

    lazy val api = new ItemApi {

      override def transform: (Item, Option[String]) => JsValue = transformItemToJson

      def transformItemToJson(item: Item, detail:Option[String] = None):JsValue = {
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

      override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = loadForRead.map(_ => OrgAndOpts(ObjectId.get, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken))
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
        contentAsJson(result) === api.transformItemToJson(expectedItem)
      }

    }

  }
}
