package org.corespring.v2player.integration.controllers.editor

import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2player.integration.actionBuilders.AuthenticatedItem
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc.Results._
import play.api.mvc.{ SimpleResult, RequestHeader }
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.json.Json
import org.specs2.specification.Before
import org.bson.types.ObjectId

class AssetActionsTest extends Specification with Mockito {

  "AssetActions" should {

    def makeAction(authResult: Option[SimpleResult], deleteResponse: DeleteResponse) = {

      val mockAuthForItem: AuthenticatedItem = mock[AuthenticatedItem]

      mockAuthForItem.authenticationFailedResult(anyString, any[RequestHeader]) returns authResult

      val actions: AssetActions = new AssetActions {

        override def s3: S3Service = {
          val m = mock[S3Service]
          m.delete(anyString, anyString) returns deleteResponse
          m
        }

        override def authForItem: AuthenticatedItem = mockAuthForItem

        override def bucket: String = ""

        override def itemService: ItemService = mock[ItemService]
      }
      actions
    }

    class deleteScope(authResult: Option[SimpleResult] = None, deleteResponse: DeleteResponse = DeleteResponse(true, "", "")) extends Before {
      val actions: AssetActions = makeAction(authResult, deleteResponse)
      val result = actions.delete(anyString, anyString) { request => Ok("done!") }(FakeRequest("", ""))

      override def before: Any = {

      }
    }

    class uploadScope(itemId: String = ObjectId.get.toString, authResult: Option[SimpleResult] = None, deleteResponse: DeleteResponse = DeleteResponse(true, "", "")) extends Before {
      val actions: AssetActions = makeAction(authResult, deleteResponse)
      val result = actions.upload(anyString, anyString) { request => Ok("done!") }(FakeRequest("", ""))
      override def before: Any = {
      }
    }

    "delete with no auth error" in new deleteScope {
      contentAsString(result) === "done!"
    }

    "delete fails and returns an auth error" in new deleteScope(Some(BadRequest("Bad!"))) {
      contentAsString(result) === "Bad!"
    }

    "delete fails if s3 delete fails" in new deleteScope(None, DeleteResponse(false, "", "S3 Message")) {
      contentAsJson(result) === Json.obj("error" -> "S3 Message")
    }

    //TODO: Upload tests
    /*
    "uploads" in new uploadScope {
      contentAsString(result) === Ok("url")
    }*/
  }
}
