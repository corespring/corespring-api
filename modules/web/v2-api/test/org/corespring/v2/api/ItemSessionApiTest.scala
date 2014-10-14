package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class ItemSessionApiTest extends Specification with Mockito {

  class apiScope(
    val canCreate: Validation[V2Error, Boolean] = Failure(generalError("no")),
    val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(OrgAndOpts(ObjectId.get, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken)),
    val maybeSessionId: Option[ObjectId] = None,
    val sessionAndItem: Validation[V2Error, (JsValue, Item)] = Failure(generalError("no"))) extends Scope {

    val api: ItemSessionApi = new ItemSessionApi {
      override def sessionAuth: SessionAuth[OrgAndOpts] = {
        val m = mock[SessionAuth[OrgAndOpts]]
        m.canCreate(anyString)(any[OrgAndOpts]) returns canCreate
        m.loadForRead(anyString)(any[OrgAndOpts]) returns sessionAndItem
        m
      }

      override def sessionService: MongoService = {
        val m = mock[MongoService]
        m.create(any[JsValue]) returns maybeSessionId
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

      /**
       * A session has been created for an item with the given item id.
       * @param itemId
       */
      override def sessionCreatedForItem(itemId: VersionedId[ObjectId]): Unit = {}

      override def outcomeProcessor: OutcomeProcessor = {
        val m = mock[OutcomeProcessor]
        m
      }

      override def scoreProcessor: ScoreProcessor = {
        val m = mock[ScoreProcessor]
        m
      }
    }
  }

  "V2 - ItemSessionApi" should {

    "when calling create" should {
      "fail when auth fails" in new apiScope() {
        val result = api.create(VersionedId(ObjectId.get))(FakeRequest().withBody(Some()))
        status(result) === BAD_REQUEST
        contentAsJson(result) === Json.obj("errorType" -> "generalError", "message" -> "no")
      }

      "fail when service fails" in new apiScope(Success(true)) {
        val result = api.create(VersionedId(ObjectId.get))(FakeRequest().withBody(Some()))
        status(result) === BAD_REQUEST
        (contentAsJson(result) \ "errorType").as[String] === "errorSaving"
      }

      "work" in new apiScope(
        Success(true),
        maybeSessionId = Some(ObjectId.get)) {
        val result = api.create(VersionedId(ObjectId.get))(FakeRequest("", "").withBody(Some()))
        status(result) === OK
        contentAsJson(result) === Json.obj("id" -> maybeSessionId.get.toString)
      }

      "work with json header, but no body" in new apiScope(
        Success(true),
        maybeSessionId = Some(ObjectId.get)) {
        val result = api.create(VersionedId(ObjectId.get))(FakeRequest("", "")
          .withHeaders(("Content-Type", "application/json")).withBody(None))
        status(result) === OK
        contentAsJson(result) === Json.obj("id" -> maybeSessionId.get.toString)
      }
    }

    "when calling get" should {
      "fail when auth load fails" in new apiScope() {
        val result = api.get("sessionId")(FakeRequest("", ""))
        status(result) === BAD_REQUEST
      }

      "work" in new apiScope(
        sessionAndItem = Success((Json.obj(), Item()))) {
        val result = api.get("sessionId")(FakeRequest("", ""))
        status(result) === OK
      }
    }
  }

}