package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.itemSearch.{ ItemIndexQuery, ItemIndexSearchResult }
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts }
import org.corespring.v2.errors.Errors.{ cantParseItemId, generalError }
import org.corespring.v2.errors.V2Error
import play.api.libs.json.Json
import play.api.mvc.{Request, AnyContent, AnyContentAsJson}
import play.api.test.{FakeHeaders, FakeRequest}

import scala.concurrent._
import scalaz.{ Failure, Success, Validation }

class ItemApiCloneTest extends ItemApiSpec {

  import ExecutionContext.Implicits.global

  "ItemApi" should {

    val req = FakeRequest("", "")

    "when calling clone" should {

      lazy val orgAndOpts = mockOrgAndOpts(AuthMode.UserSession)

      lazy val vid = VersionedId(ObjectId.get)
      lazy val clonedId = VersionedId(ObjectId.get)

      lazy val clonedItem: Item = Item(id = clonedId, collectionId = ObjectId.get.toString)

      case class ItemApiCloneScope(vid: String = vid.toString,
        id: Validation[V2Error, OrgAndOpts] = Success(orgAndOpts),
        itemServiceClones: Boolean = true,
        item: Option[Item] = Some(clonedItem)) extends ItemApiScope {

        cloneItemService.cloneItem(any[VersionedId[ObjectId]], any[ObjectId], any[Option[ObjectId]]) returns {
          if (itemServiceClones) Success(clonedId) else Failure(PlatformServiceError("cloneItem failed"))
        }

        itemIndexService.search(any[ItemIndexQuery]) returns future { Success(ItemIndexSearchResult.empty) }

        override val orgAndOpts: Validation[V2Error, OrgAndOpts] = id

        def result(r:Request[AnyContent]) = api.cloneItem(vid.toString)(r)
      }

      "return 200" in new ItemApiCloneScope {
        status(result(req)) === OK
      }

      s"return $BAD_REQUEST if a bad collection id is passed in" in new ItemApiCloneScope(
      ){
        val request = FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("collectionId" -> "bad-id")))
        val r = result(request)
        status(r) must_== BAD_REQUEST
      }

      "return the cloned id" in new ItemApiCloneScope {
        (contentAsJson(result(req)) \ "id").as[String] === clonedId.toString
      }

      val testError = generalError("test error")

      "fail if there's no identity" in new ItemApiCloneScope(id = Failure(testError)) {
        status(result(req)) === testError.statusCode
      }

      "fail if the item id is unparseable" in new ItemApiCloneScope(vid = "?") {
        val r = result(req)
        status(r) === testError.statusCode
        (contentAsJson(r) \ "message").as[String] === cantParseItemId("?").message
      }

      "fail if clone fails" in new ItemApiCloneScope(itemServiceClones = false) {
        val r = result(req)
        status(r) === testError.statusCode
        val json = contentAsJson(r)
        (json \ "message").as[String] must contain("Error cloning")
      }
    }
  }
}
