package org.corespring.v2.api

import scala.concurrent.ExecutionContext

import org.bson.types.ObjectId
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.errors.Errors.{ generalError, noJson, sessionDoesNotContainResponses }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, RequestHeader }
import play.api.test.{ FakeHeaders, FakeRequest }
import play.api.test.Helpers._
import scalaz.{ Failure, Success, Validation }

class ItemSessionApiTest extends Specification with Mockito {

  class apiScope(
    val canCreate: Validation[V2Error, Boolean] = Failure(generalError("no")),
    val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(OrgAndOpts(ObjectId.get, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken)),
    val maybeSessionId: Option[ObjectId] = None,
    val sessionAndItem: Validation[V2Error, (JsValue, Item)] = Failure(generalError("no session and item"))) extends Scope {

    val api: ItemSessionApi = new ItemSessionApi {
      override def sessionAuth: SessionAuth[OrgAndOpts] = {
        val m = mock[SessionAuth[OrgAndOpts]]
        m.canCreate(anyString)(any[OrgAndOpts]) returns canCreate
        m.loadForRead(anyString)(any[OrgAndOpts]) returns sessionAndItem
        m.loadForWrite(anyString)(any[OrgAndOpts]) returns sessionAndItem
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
        m.createOutcome(any[JsValue], any[JsValue], any[JsValue]) returns Json.obj("outcome" -> "?")
        m
      }

      override def scoreProcessor: ScoreProcessor = {
        val m = mock[ScoreProcessor]
        m.score(any[JsValue], any[JsValue], any[JsValue]) returns Json.obj("score" -> "?")
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

    "when calling check score" should {

      "fail when there is no json in the request body" in new apiScope(
        canCreate = Success(true)) {
        val result = api.checkScore("sessionId")(FakeRequest("", ""))
        status(result) === noJson.statusCode
        contentAsJson(result) === noJson.json
      }

      "fail when session and item are not found" in new apiScope() {
        val result = api.checkScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = sessionAndItem.toEither.left.get
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "fail when the item has no player definition" in new apiScope(
        sessionAndItem = Success(Json.obj(), Item())) {
        val result = api.checkScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = generalError("This item has no player definition")
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "work" in new apiScope(
        sessionAndItem = Success(Json.obj(), Item(playerDefinition = Some(
          PlayerDefinition(
            files = Seq.empty,
            xhtml = "<html></html>",
            components = Json.obj(),
            summaryFeedback = "?",
            customScoring = None))))) {
        val result = api.checkScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = generalError("This item has no player definition")
        status(result) === OK
        contentAsJson(result) === Json.obj("score" -> "?")
      }
    }

    "when calling load score" should {

      "fail when session and item are not found" in new apiScope() {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = sessionAndItem.toEither.left.get
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "fail when the session has no 'components'" in new apiScope(
        sessionAndItem = Success(Json.obj(), Item())) {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = sessionDoesNotContainResponses("sessionId")
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "fail when the item has no player definition" in new apiScope(
        sessionAndItem = Success(Json.obj("components" -> Json.obj()), Item())) {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = generalError("This item has no player definition")
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "work" in new apiScope(
        sessionAndItem = Success(Json.obj("components" -> Json.obj()), Item(playerDefinition = Some(
          PlayerDefinition(
            files = Seq.empty,
            xhtml = "<html></html>",
            components = Json.obj(),
            summaryFeedback = "?",
            customScoring = None))))) {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = generalError("This item has no player definition")
        status(result) === OK
        contentAsJson(result) === Json.obj("score" -> "?")
      }
    }
  }
}