package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.item._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.libs.json.Json

import scalaz.{ Failure, Success, Validation }

class ItemApiGetTest extends ItemApiSpec {

  val collectionId = ObjectId.get()

  case class getApiScope(
    defaultCollectionId: ObjectId = collectionId,
    loadForRead: Validation[V2Error, Item] = Failure(notReady)) extends ItemApiScope {

    itemAuth.loadForRead(anyString)(any[OrgAndOpts]) returns loadForRead

    override def orgAndOpts: Validation[V2Error, OrgAndOpts] = loadForRead.map(_ => mockOrgAndOpts())
  }

  "V2 - ItemApi" should {

    "when calling get" should {

      s"returns $UNAUTHORIZED - if permission denied" in new getApiScope(
        loadForRead = Failure(generalError("Nope", UNAUTHORIZED))) {

        val id = VersionedId(ObjectId.get)
        val result = api.get(id.toString())(FakeJsonRequest(Json.obj()))
        val e = loadForRead.toEither.left.get
        result must beCodeAndJson(e.statusCode, e.json)
      }

      "returns item" in new getApiScope(
        loadForRead = Success(new Item(id = VersionedId(ObjectId.get), collectionId = collectionId.toString))) {
        val expectedItem = loadForRead.toEither.right.get
        val result = api.get(expectedItem.id.toString)(FakeJsonRequest(Json.obj()))
        import scala.language.reflectiveCalls
        contentAsJson(result) === jsonFormatting.itemSummary.write(expectedItem, None)
      }

    }

  }
}
