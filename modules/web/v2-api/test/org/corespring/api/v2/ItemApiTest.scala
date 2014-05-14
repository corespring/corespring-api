package org.corespring.api.v2

import org.bson.types.ObjectId
import org.corespring.api.v2.actions.OrgRequest
import org.corespring.api.v2.actions.V2ItemActions
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{JsObject, Json, JsValue}
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, ExecutionContext}
import org.corespring.api.v2.errors.Errors.invalidJson

class ItemApiTest extends Specification with Mockito {

  /**
   * We should only need to run FakeApplication here.
   * However the way the app is tied up - we need to boot a real application.
   */
  PlaySingleton.start()

  def FakeJsonRequest(json: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("", "", FakeHeaders(), AnyContentAsJson(json))

  case class apiScope(val defaultCollectionId: ObjectId = ObjectId.get) extends Scope {
    lazy val api = new ItemApi {
      override def itemService: ItemService = {
        val m = mock[ItemService]
        m.insert(any[Item]).returns(Some(VersionedId(ObjectId.get)))
        m
      }

      override def itemActions: V2ItemActions[AnyContent] = new V2ItemActions[AnyContent] {

        override def create(block: (OrgRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action.async {
          r =>
            block(OrgRequest(r, ObjectId.get, defaultCollectionId))
        }
      }

      override implicit def executionContext: ExecutionContext = global
    }
  }

  "V2 - ItemApi" should {

    "create - ignores the id in the request" in new apiScope {
      val result = api.create()(FakeJsonRequest(Json.obj("id" -> "blah")))
      (contentAsJson(result) \ "id").as[String] !== "blah"
      (contentAsJson(result) \ "collectionId").as[String] === defaultCollectionId.toString
      status(result) === OK
    }

    "create - if there's no json in the body - it uses the default json" in new apiScope {
      val result = api.create()(FakeRequest("", ""))
      (contentAsJson(result) \ "collectionId").as[String] === defaultCollectionId.toString
      (contentAsJson(result) \ "playerDefinition").as[JsObject] === api.defaultPlayerDefinition
      status(result) === OK
    }

    "create - if there's no player definition in the json body - it adds the default player definition" in new apiScope {
      val customCollectionId = ObjectId.get
      val result = api.create()(FakeJsonRequest(Json.obj("collectionId" -> customCollectionId.toString)))
      (contentAsJson(result) \ "collectionId").as[String] === customCollectionId.toString
      (contentAsJson(result) \ "playerDefinition").as[JsObject] === api.defaultPlayerDefinition
      status(result) === OK
    }

    s"create - returns $BAD_REQUEST for invalid json" in new apiScope {
      val customCollectionId = ObjectId.get
      val result = api.create()(
        FakeRequest("", "", FakeHeaders(), AnyContentAsText("arst"))
      )
      status(result) === invalidJson("arst").code
      contentAsString(result) === invalidJson("arst").message
    }
  }
}
