package org.corespring.api.v2

import org.corespring.api.v2.errors.V2ApiError

import scala.concurrent.{ ExecutionContext, Future }

import org.bson.types.ObjectId
import org.corespring.api.v2.actions.{ OrgRequest, V2ApiActions }
import org.corespring.api.v2.errors.Errors.{ errorSaving, unAuthorized }
import org.corespring.api.v2.services._
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.services.OrgService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._
import play.api.test.{ FakeHeaders, FakeRequest }
import play.api.test.Helpers._
import scalaz.{ Failure, Success, Validation }

class ItemApiTest extends Specification with Mockito {

  /**
   * We should not need to run the app for a unit test.
   * However the way the app is tied up (global Dao Objects) - we need to boot a play application.
   */
  PlaySingleton.start()

  def FakeJsonRequest(json: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("", "", FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsJson(json))

  case class apiScope(
    val defaultCollectionId: ObjectId = ObjectId.get,
    val insertFails: Boolean = false,
    val permissionResult: Validation[V2ApiError, Item] = Success(Item()),
    val loadOrgResult: Option[Organization] = Some(Organization())) extends Scope {
    lazy val api = new ItemApi {
      override def itemService: ItemService = {
        val m = mock[ItemService]
        val out = if (insertFails) None else Some(VersionedId(ObjectId.get))
        m.insert(any[Item]).returns(out)
        m
      }

      override def actions: V2ApiActions[AnyContent] = new V2ApiActions[AnyContent] {

        override def orgAction(bp: BodyParser[AnyContent])(block: (OrgRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action.async {
          r =>
            block(OrgRequest(r, ObjectId.get, defaultCollectionId))
        }
      }

      override def permissionService: PermissionService[Organization, Item] = {
        val m = mock[PermissionService[Organization, Item]]
        m.create(any[Organization], any[Item]) returns permissionResult
        m
      }

      override def orgService: OrgService = {
        val m = mock[OrgService]
        m.org(any[ObjectId]) returns loadOrgResult
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global
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
      val result = api.create()(FakeJsonRequest(Json.obj()))
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

    s"create - returns $OK - it ignores bad json, if it can't be parsed" in new apiScope {
      //TODO - is this desired?
      val customCollectionId = ObjectId.get
      val result = api.create()(
        FakeRequest("", "", FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsText("bad")))
      status(result) === OK
    }

    s"returns $UNAUTHORIZED - if permisssion denied" in new apiScope(
      permissionResult = Failure(unAuthorized("Nope"))) {
      val result = api.create()(FakeJsonRequest(Json.obj()))
      status(result) === unAuthorized("Nope").code
      contentAsJson(result) === Json.obj("error" -> unAuthorized("Nope").message)

    }

    s"create - returns error with a bad save" in new apiScope(
      insertFails = true) {
      val result = api.create()(FakeJsonRequest(Json.obj()))
      status(result) === errorSaving.code
      contentAsJson(result) === Json.obj("error" -> errorSaving.message)
    }

  }
}
