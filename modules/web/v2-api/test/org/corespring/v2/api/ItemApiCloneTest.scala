package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.itemSearch.{ ItemIndexQuery, ItemIndexSearchResult }
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts }
import org.corespring.v2.errors.Errors.{ cantParseItemId, generalError }
import org.corespring.v2.errors.V2Error
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsJson}
import play.api.test.{FakeHeaders, FakeRequest}

import scala.concurrent._
import scalaz.{ Failure, Success, Validation }

class ItemApiCloneTest extends ItemApiSpec {

  import ExecutionContext.Implicits.global

  "ItemApi" should {
    "when calling clone" should {

      lazy val orgAndOpts = mockOrgAndOpts(AuthMode.UserSession)

      lazy val vid = VersionedId(ObjectId.get)
      lazy val clonedId = VersionedId(ObjectId.get)

      lazy val clonedItem: Item = Item(id = clonedId, collectionId = ObjectId.get.toString)

      case class ItemApiCloneScope(vid: String = vid.toString,
        id: Validation[V2Error, OrgAndOpts] = Success(orgAndOpts),
        itemAuthLoadsItem: Boolean = true,
        itemServiceClones: Boolean = true,
        item: Option[Item] = Some(clonedItem),
        request : FakeRequest[Any] = FakeRequest("", "")) extends ItemApiScope {

        itemService.clone(any[Item]) returns (if (itemServiceClones) item else None)

        itemIndexService.search(any[ItemIndexQuery]) returns future { Success(ItemIndexSearchResult.empty) }

        itemAuth.loadForRead(anyString)(any[OrgAndOpts]) returns {
          import scalaz.Scalaz._
          val out = if (itemAuthLoadsItem) item else None
          out.toSuccess(generalError("Test error: itemAuth.loadForRead"))
        }

        override val orgAndOpts: Validation[V2Error, OrgAndOpts] = id

        lazy val result = api.cloneItem(vid.toString)(FakeRequest("", ""))
      }

      "return 200" in new ItemApiCloneScope {
        status(result) === OK
      }

      s"return $BAD_REQUEST if a bad collection id is passed in" in new ItemApiCloneScope(
        request = FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("collectionId" -> "bad-id")))
      ){
        status(result) must_== BAD_REQUEST
      }

      "return the cloned id" in new ItemApiCloneScope {
        (contentAsJson(result) \ "id").as[String] === clonedId.toString
      }

      val testError = generalError("test error")

      "fail if there's no identity" in new ItemApiCloneScope(id = Failure(testError)) {
        status(result) === testError.statusCode
      }

      "fail if the item id is unparseable" in new ItemApiCloneScope(vid = "?") {
        status(result) === testError.statusCode
        (contentAsJson(result) \ "message").as[String] === cantParseItemId("?").message
      }

      "fail if clone fails" in new ItemApiCloneScope(itemServiceClones = false) {
        status(result) === testError.statusCode
        val json = contentAsJson(result)
        (json \ "message").as[String] must contain("Error cloning")
      }
    }
  }
}
