package org.corespring.api.v2

import org.bson.types.ObjectId
import org.corespring.api.v2.actions.OrgRequest
import org.corespring.api.v2.actions.V2ItemActions
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.{Json, JsValue}
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, ExecutionContext}
import org.corespring.test.PlaySingleton

class ItemApiTest extends Specification with Mockito{

  /**
   * We should only need to run FakeApplication here.
   * However the way the app is tied up - we need to boot a real application.
   */
  PlaySingleton.start()

  def FakeJsonRequest(json:JsValue) : FakeRequest[AnyContentAsJson] = FakeRequest("", "", FakeHeaders(), AnyContentAsJson(json))

  val api = new ItemApi {
    override def itemService: ItemService = {
      val m = mock[ItemService]
      m.insert(any[Item]).returns(Some(VersionedId(ObjectId.get)))
      m
    }

    override def itemActions: V2ItemActions[AnyContent] = new V2ItemActions[AnyContent] {

      override def create(block: (OrgRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action.async{
        r =>
          block(OrgRequest(r, ObjectId.get, ObjectId.get))
      }
    }
    override implicit def executionContext: ExecutionContext = global
  }

  "V2 - ItemApi" should {
    "create ignores the id in the request" in {
      val result = api.create()(FakeJsonRequest(Json.obj("id" -> "blah")))
      println(contentAsString(result))
      status(result) === OK
    }
  }
}
