package org.corespring.v2.api

import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.api.v1.{ItemApi => V1ItemApi}
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.{PlaySpecification, FakeRequest}

import scala.concurrent.Future

class CmsTest extends Specification with Mockito with PlaySpecification{


  case class CmsScope(v1Result : SimpleResult) extends Scope{

    lazy val mockCollection = {
      val m = mock[MongoCollection]
      m
    }

    lazy val mockTransformer =  mock[ItemTransformer]

    lazy val cms = new Cms {

      override def itemTransformer: ItemTransformer = mockTransformer

      override def itemCollection: MongoCollection = mockCollection

      override def v1ApiCreate = (req) => {
        import scala.concurrent.ExecutionContext.Implicits.global
        Future{v1Result}
      }
    }
  }

  "Cms" should {

    "when calling createItemFromV1Data" should{
      import play.api.mvc.Results._
      val oid = ObjectId.get

      val ok = Ok(Json.obj("id" -> oid.toString))
      val notOk = BadRequest(Json.obj())

      "return 200" in new CmsScope(ok) {
        val result = cms.createItemFromV1Data()(FakeRequest("", ""))
        status(result) === OK
      }

      val unset = MongoDBObject("$unset" -> MongoDBObject("data" -> ""))

      "call the mongoCollection.update if status == 200" in new CmsScope(ok){
        cms.createItemFromV1Data()(FakeRequest("",""))
        there was one(mockCollection).update(
          MongoDBObject("_id._id" -> oid),  unset, false, false)
      }

      "not call the mongoCollection.update if ?keep-v1-data=true" in new CmsScope(ok){
        cms.createItemFromV1Data()(FakeRequest("","?keep-v1-data=true"))
        there was no(mockCollection).update(
          MongoDBObject("_id._id" -> oid), unset, false, false)
      }

      "not call the mongoCollection.update if status != 200" in new CmsScope(notOk){
        cms.createItemFromV1Data()(FakeRequest("",""))
        there was no(mockCollection).update(
          MongoDBObject("_id._id" -> oid), unset, false, false)
      }

      "call the itemTransformer if status == 200" in new CmsScope(ok){
        cms.createItemFromV1Data()(FakeRequest("",""))
        there was one(mockTransformer).updateV2Json(VersionedId(oid))
      }

      "not call the itemTransformer is status != 200" in new CmsScope(notOk){
        cms.createItemFromV1Data()(FakeRequest("",""))
        there was no(mockTransformer).updateV2Json(VersionedId(oid))
      }
    }
  }

}
