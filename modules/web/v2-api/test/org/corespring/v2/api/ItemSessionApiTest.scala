package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.{AuthMode, MockFactory, OrgAndOpts, PlayerAccessSettings}
import org.corespring.v2.errors.Errors.{generalError, sessionDoesNotContainResponses}
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, RequestHeader}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}

import scala.concurrent.ExecutionContext
import scalaz.{Failure, Success, Validation}

class ItemSessionApiTest extends Specification with Mockito with MockFactory {

  class apiScope(
    val canCreate: Validation[V2Error, Boolean] = Failure(generalError("no")),
    val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(OrgAndOpts(mockOrg, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken)),
    val maybeSessionId: Option[ObjectId] = None,
    val sessionAndItem: Validation[V2Error, (JsValue, PlayerDefinition)] = Failure(generalError("no")),
    val scoreResult: Validation[V2Error, JsValue] = Failure(generalError("error getting score"))) extends Scope {

    val api: ItemSessionApi = new ItemSessionApi {
      override def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = {
        val m = mock[SessionAuth[OrgAndOpts, PlayerDefinition]]
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

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

      /**
       * A session has been created for an item with the given item id.
       * @param itemId
       */
      override def sessionCreatedForItem(itemId: VersionedId[ObjectId]): Unit = {}

      override def scoreService: ScoreService = {
        val m = mock[ScoreService]
        m.score(any[PlayerDefinition], any[JsValue]) returns scoreResult
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
        sessionAndItem = Success((Json.obj(), new PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))) {
        val result = api.get("sessionId")(FakeRequest("", ""))
        status(result) === OK
      }
    }

    "when calling load score" should {

      "fail when session and item are not found" in new apiScope() {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = sessionAndItem.toEither.left.get
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      def emptyPlayerDefinition = PlayerDefinition(Seq.empty, "", Json.obj(), "", None)
      "fail when the session has no 'components'" in new apiScope(
        sessionAndItem = Success(Json.obj(), emptyPlayerDefinition)) {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = sessionDoesNotContainResponses("sessionId")
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "work" in new apiScope(
        sessionAndItem = Success(
          Json.obj("components" -> Json.obj()),
          emptyPlayerDefinition),
        scoreResult = Success(Json.obj("score" -> 100))) {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = generalError("This item has no player definition")
        status(result) === OK
        contentAsJson(result) === Json.obj("score" -> 100)
      }
    }
  }
}