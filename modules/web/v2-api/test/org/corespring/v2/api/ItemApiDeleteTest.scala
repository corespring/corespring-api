package org.corespring.v2.api

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item.Keys._
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.services.item.{ItemIndexService, ItemService}
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

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class ItemApiDeleteTest extends Specification with Mockito with MockFactory {

  /**
   * We should not need to run the app for a unit test.
   * However the way the app is tied up (global Dao Objects) - we need to boot a play application.
   */
  PlaySingleton.start()

  def FakeJsonRequest(json: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("", "", FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsJson(json))

  case class deleteApiScope(isLoggedIn: Boolean = true,
    findFieldsById: Option[DBObject] = None,
    canDelete: Boolean = false,
    throwErrorInMoveItemToArchive: Boolean = false) extends Scope {

    val dummyOrgId = ObjectId.get
    val dummyCollectionId = ObjectId.get.toString

    lazy val api = new ItemApi {

      override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = ???

      override def getSummaryData: (Item, Option[String]) => JsValue = transformItemToJson

      private def transformItemToJson(item: Item, detail: Option[String]): JsValue = {
        Json.toJson(item)
      }

      private def canDeleteResult(): Validation[V2Error, Boolean] =
        if (canDelete)
          Success(true)
        else
          Failure(orgCantAccessCollection(dummyOrgId, dummyCollectionId, Permission.Write.name))

      override def itemService: ItemService = {
        val m = mock[ItemService]
        m.findFieldsById(any, any) returns findFieldsById
        if (throwErrorInMoveItemToArchive)
          org.mockito.Mockito.doThrow(new RuntimeException("Mock Error")).when(m).moveItemToArchive(any)
        m
      }

      override def itemAuth: ItemAuth[OrgAndOpts] = {
        val m = mock[ItemAuth[OrgAndOpts]]
        m.canCreateInCollection(anyString)(any[OrgAndOpts]) returns canDeleteResult()
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] =
        if (isLoggedIn)
          Success(OrgAndOpts(mockOrg(), PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, None))
        else
          Failure(unAuthorized(""))

      override def scoreService: ScoreService = {
        val m = mock[ScoreService]
        m
      }

      override def itemIndexService: ItemIndexService = ???
    }
  }

  "V2 - ItemApi" should {

    "when calling delete" should {

      s"returns cantParseItemId - if itemId is invalid" in new deleteApiScope() {
        val result = api.delete("123")(FakeJsonRequest(Json.obj()))
        val e = cantParseItemId("123")
        status(result) === e.statusCode
        contentAsJson(result) === e.json
      }

      s"returns unAuthorised - if not logged in" in new deleteApiScope(
        isLoggedIn = false) {
        val itemId = VersionedId(ObjectId.get)
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = unAuthorized("")
        status(result) === e.statusCode
        contentAsJson(result) === e.json
      }

      s"returns cantFindItemWithId - if item does not exist" in new deleteApiScope() {
        val itemId = VersionedId(ObjectId.get)
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = cantFindItemWithId(itemId)
        status(result) === e.statusCode
        contentAsJson(result) === e.json
      }

      s"returns orgCantAccessCollection - if not allowed to delete" in new deleteApiScope(
        findFieldsById = Some(MongoDBObject(collectionId -> "123")),
        canDelete = false) {
        val itemId = VersionedId(ObjectId.get)
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = orgCantAccessCollection(dummyOrgId, dummyCollectionId, Permission.Write.name)
        contentAsJson(result) === e.json
        status(result) === e.statusCode
      }

      s"returns general error - if error in delete" in new deleteApiScope(
        findFieldsById = Some(MongoDBObject(collectionId -> "123")),
        canDelete = true,
        throwErrorInMoveItemToArchive = true) {
        val itemId = VersionedId(ObjectId.get)
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = generalError(s"Error deleting item ${itemId.toString}")
        contentAsJson(result) === e.json
        status(result) === e.statusCode
      }

      s"work" in new deleteApiScope(
        findFieldsById = Some(MongoDBObject(collectionId -> "123")),
        canDelete = true) {
        val itemId = VersionedId(ObjectId.get)
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        status(result) === OK
      }

    }

  }
}
