package org.corespring.v2.api

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item.Keys._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.mockito.{ Mockito => OrigMockito }
import play.api.libs.json.Json
import play.api.test.Helpers._

import scalaz.{ Failure, Success, Validation }

class ItemApiDeleteTest extends ItemApiSpec {

  /**
   * We should not need to run the app for a unit test.
   * However the way the app is tied up (global Dao Objects) - we need to boot a play application.
   */
  PlaySingleton.start()

  case class deleteApiScope(isLoggedIn: Boolean = true,
    findFieldsById: Option[DBObject] = None,
    canDelete: Boolean = false,
    throwErrorInMoveItemToArchive: Boolean = false) extends ItemApiScope {

    val dummyOrgId = ObjectId.get
    val dummyCollectionId = ObjectId.get.toString

    mockItemService.findFieldsById(any, any) returns findFieldsById

    if (throwErrorInMoveItemToArchive) {
      OrigMockito.doThrow(
        new RuntimeException("Mock Error")).when(mockItemService).moveItemToArchive(any)
    }

    mockItemAuth.canCreateInCollection(anyString)(any[OrgAndOpts]) returns {
      if (canDelete)
        Success(true)
      else
        Failure(orgCantAccessCollection(dummyOrgId, dummyCollectionId, Permission.Write.name))
    }

    override def orgAndOpts: Validation[V2Error, OrgAndOpts] =
      if (isLoggedIn)
        Success(OrgAndOpts(mockOrg(), PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, None))
      else
        Failure(unAuthorized(""))
  }

  "V2 - ItemApi" should {

    "when calling delete" should {

      s"returns cantParseItemId - if itemId is invalid" in new deleteApiScope() {
        val result = api.delete("123")(FakeJsonRequest(Json.obj()))
        val e = cantParseItemId("123")
        result must beCodeAndJson(e.statusCode, e.json)
      }

      s"returns unAuthorised - if not logged in" in new deleteApiScope(
        isLoggedIn = false) {
        val itemId = VersionedId(ObjectId.get)
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = unAuthorized("")
        result must beCodeAndJson(e.statusCode, e.json)
      }

      s"returns cantFindItemWithId - if item does not exist" in new deleteApiScope() {
        val itemId = VersionedId(ObjectId.get)
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = cantFindItemWithId(itemId)
        result must beCodeAndJson(e.statusCode, e.json)
      }

      s"returns orgCantAccessCollection - if not allowed to delete" in new deleteApiScope(
        findFieldsById = Some(MongoDBObject(collectionId -> "123")),
        canDelete = false) {
        val itemId = VersionedId(ObjectId.get)
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = orgCantAccessCollection(dummyOrgId, dummyCollectionId, Permission.Write.name)
        result must beCodeAndJson(e.statusCode, e.json)
      }

      s"returns general error - if error in delete" in new deleteApiScope(
        findFieldsById = Some(MongoDBObject(collectionId -> "123")),
        canDelete = true,
        throwErrorInMoveItemToArchive = true) {
        val itemId = VersionedId(ObjectId.get)
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = generalError(s"Error deleting item ${itemId.toString}")
        result must beCodeAndJson(e.statusCode, e.json)
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
