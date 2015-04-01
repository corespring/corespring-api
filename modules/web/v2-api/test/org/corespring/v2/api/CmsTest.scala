package org.corespring.v2.api

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.api.v1.{ ItemApi => V1ItemApi }
import org.corespring.drafts.item.ItemDrafts
import org.corespring.platform.core.models.User
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.services.OrgService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.{ FakeRequest, PlaySpecification }

import scala.concurrent.Future

class CmsTest extends Specification with Mockito with PlaySpecification {

  trait BaseCms extends Cms {
    override def itemTransformer: ItemTransformer = ???

    override def itemService: ItemService = ???

    override def identifyUser(rh: RequestHeader): Option[User] = ???

    override def v1ApiCreate: (Request[AnyContent]) => Future[SimpleResult] = ???

    override def itemDrafts: ItemDrafts = ???

    override def orgService: OrgService = ???
  }

  "Cms" should {

    "when calling item-format" should {

      val vid = new VersionedId(ObjectId.get)

      case class ItemFormatScope(item: Option[Item] = None) extends Scope {

        lazy val cms = new BaseCms {

          override lazy val itemService: ItemService = {
            val m = mock[ItemService]
            m.findOneById(any[VersionedId[ObjectId]]) returns item
            m
          }

        }
      }

      val v1Item = {
        val m = mock[Item]
        m.hasQti returns true
        m.hasPlayerDefinition returns true
        m.createdByApiVersion returns 1
        m
      }

      "return the format" in new ItemFormatScope(Some(v1Item)) {
        val result = cms.getItemFormat(vid.toString)(FakeRequest("", ""))
        there was one(cms.itemService).findOneById(vid)
        contentAsJson(result) === Json.obj("hasQti" -> true, "hasPlayerDefinition" -> true, "apiVersion" -> 1)
      }

      "return 404 - when item isn't in db" in new ItemFormatScope() {
        val result = cms.getItemFormat(vid.toString)(FakeRequest("", ""))
        there was one(cms.itemService).findOneById(vid)
        status(result) === NOT_FOUND
      }

      "return 404 for bad versioned id" in new ItemFormatScope() {
        val result = cms.getItemFormat("?")(FakeRequest("", ""))
        there was no(cms.itemService).findOneById(vid)
        status(result) === NOT_FOUND
      }

    }

    "when calling createItemFromV1Data" should {

      case class CmsScope(v1Result: SimpleResult) extends Scope {

        lazy val mockCollection = mock[MongoCollection]

        lazy val cms = new BaseCms {

          override lazy val itemTransformer: ItemTransformer = mock[ItemTransformer]

          override val itemService: ItemService = {
            val m = mock[ItemService]
            m.collection returns mockCollection
            m
          }

          override def v1ApiCreate = (req) => {
            import scala.concurrent.ExecutionContext.Implicits.global
            Future { v1Result }
          }
        }
      }

      import play.api.mvc.Results._
      val oid = ObjectId.get

      val ok = Ok(Json.obj("id" -> oid.toString))
      val notOk = BadRequest(Json.obj())

      "return 200" in new CmsScope(ok) {
        val result = cms.createItemFromV1Data()(FakeRequest("", ""))
        status(result) === OK
      }

      val unset = MongoDBObject("$unset" -> MongoDBObject("data" -> ""))

      "call the mongoCollection.update if status == 200" in new CmsScope(ok) {
        val result = cms.createItemFromV1Data()(FakeRequest("", ""))
        status(result) === OK
        there was one(mockCollection).update(
          MongoDBObject("_id._id" -> oid), unset, false, false)
      }

      "not call the mongoCollection.update if ?keep-v1-data=true" in new CmsScope(ok) {
        cms.createItemFromV1Data()(FakeRequest("", "?keep-v1-data=true"))
        there was no(mockCollection).update(
          MongoDBObject("_id._id" -> oid), unset, false, false)
      }

      "not call the mongoCollection.update if status != 200" in new CmsScope(notOk) {
        cms.createItemFromV1Data()(FakeRequest("", ""))
        there was no(mockCollection).update(
          MongoDBObject("_id._id" -> oid), unset, false, false)
      }

      "call the itemTransformer if status == 200" in new CmsScope(ok) {
        cms.createItemFromV1Data()(FakeRequest("", ""))
        there was one(cms.itemTransformer).updateV2Json(VersionedId(oid))
      }

      "not call the itemTransformer is status != 200" in new CmsScope(notOk) {
        cms.createItemFromV1Data()(FakeRequest("", ""))
        there was no(cms.itemTransformer).updateV2Json(VersionedId(oid))
      }
    }
  }

}
