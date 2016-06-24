package org.corespring.v2.api

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item.Keys._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.mockito.{ Mockito => OrigMockito }
import play.api.libs.json.Json

import scalaz.{ Failure, Success }

class ItemApiDeleteTest extends ItemApiSpec {

  case class deleteApiScope(
    isLoggedIn: Boolean = true,
    findFieldsById: Option[DBObject] = None,
    canDelete: Boolean = false,
    throwErrorInMoveItemToArchive: Boolean = false) extends ItemApiScope {

    val dummyOrgId = ObjectId.get
    val dummyCollectionId = ObjectId.get.toString

    itemService.collectionIdForItem(any) returns Some(new ObjectId(dummyCollectionId))

    if (throwErrorInMoveItemToArchive) {
      OrigMockito.doThrow(
        new RuntimeException("Mock Error")).when(itemService).moveItemToArchive(any)
    }

    itemAuth.canCreateInCollection(anyString)(any[OrgAndOpts]) returns {
      if (canDelete)
        Success(true)
      else
        Failure(orgCantAccessCollection(dummyOrgId, dummyCollectionId, Permission.Write.name))
    }
  }

  "V2 - ItemApi" should {

    "when calling delete" should {

      s"returns cantParseItemId - if itemId is invalid" in new deleteApiScope() {
        val result = api.delete("123")(FakeJsonRequest(Json.obj()))
        val e = cantParseItemId("123")
        result must beCodeAndJson(e.statusCode, e.json)
      }

      s"returns cantFindItemWithId - if itemService.collectionIdForItem returns None" in new deleteApiScope() {
        itemService.collectionIdForItem(any[VersionedId[ObjectId]]) returns None
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = cantFindItemWithId(itemId)
        result must beCodeAndJson(e.statusCode, e.json)
      }

      s"returns orgCantAccessCollection - if not allowed to delete" in new deleteApiScope(
        findFieldsById = Some(MongoDBObject(collectionId -> "123")),
        canDelete = false) {
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = orgCantAccessCollection(dummyOrgId, dummyCollectionId, Permission.Write.name)
        result must beCodeAndJson(e.statusCode, e.json)
      }

      s"returns general error - if error in delete" in new deleteApiScope(
        findFieldsById = Some(MongoDBObject(collectionId -> "123")),
        canDelete = true,
        throwErrorInMoveItemToArchive = true) {
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        val e = generalError(s"Error deleting item ${itemId.toString}")
        result must beCodeAndJson(e.statusCode, e.json)
      }

      s"work" in new deleteApiScope(
        findFieldsById = Some(MongoDBObject(collectionId -> "123")),
        canDelete = true) {
        val result = api.delete(itemId.toString)(FakeJsonRequest(Json.obj()))
        status(result) === OK
      }

    }

  }
}
