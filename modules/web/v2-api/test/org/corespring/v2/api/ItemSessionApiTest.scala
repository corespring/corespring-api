package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.{ ApiClientEncryptionService, EncryptionSuccess }
import org.corespring.models.auth.ApiClient
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrganizationService
import org.corespring.v2.actions.V2ActionsFactory
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class ItemSessionApiTest extends Specification with Mockito with MockFactory {

  class apiScope(
    val canCreate: Validation[V2Error, Boolean] = Failure(generalError("no")),
    val maybeSessionId: Option[ObjectId] = None,
    val sessionAndItem: Validation[V2Error, (JsValue, PlayerDefinition)] = Failure(generalError("no")),
    val scoreResult: Validation[V2Error, JsValue] = Failure(generalError("error getting score")),
    val clonedSession: Validation[V2Error, ObjectId] = Failure(generalError("no")),
    val sessionCounts: Validation[V2Error, Map[DateTime, Long]] = Success(Map.empty[DateTime, Long])) extends Scope {

    lazy val req = FakeRequest()

    val mockSessionAuth = {
      val m = mock[SessionAuth[OrgAndOpts, PlayerDefinition]]
      m.canCreate(anyString)(any[OrgAndOpts]) returns canCreate
      m.loadForRead(anyString)(any[OrgAndOpts]) returns sessionAndItem
      m.loadForWrite(anyString)(any[OrgAndOpts]) returns sessionAndItem
      m.loadWithIdentity(anyString)(any[OrgAndOpts]) returns sessionAndItem
      m.cloneIntoPreview(anyString)(any[OrgAndOpts]) returns clonedSession
      m.orgCount(any[ObjectId], any[DateTime])(any[OrgAndOpts]) returns sessionCounts
      import scalaz.Scalaz._
      m.create(any[JsValue])(any[OrgAndOpts]) returns maybeSessionId.toSuccess(errorSaving("no session id returned from mock"))
      m
    }

    val mockScoreService = {
      val m = mock[ScoreService]
      m.score(any[PlayerDefinition], any[JsValue]) returns scoreResult
      m
    }

    val mockOrgService = {
      val m = mock[OrganizationService]
      m
    }

    val mockEncryptionService = {
      val m = mock[ApiClientEncryptionService]
      m.encrypt(any[ApiClient], any[String]) returns Some(EncryptionSuccess("apiClient", "encrypted"))
      m
    }

    def sessionCreatedForItem(id: VersionedId[ObjectId]): Unit = {}

    val apiContext = ItemSessionApiExecutionContext(ExecutionContext.Implicits.global)

    val api = new ItemSessionApi(
      V2ActionsFactory.apply,
      mockSessionAuth,
      mockScoreService,
      mockOrgService,
      mockEncryptionService,
      sessionCreatedForItem,
      apiContext)
  }

  "cloneSession" should {

    "with invalid session" should {
      val missingSessionId = new ObjectId().toString

      "return 404" in new apiScope(clonedSession = Failure(cantLoadSession(missingSessionId))) {
        val result = api.cloneSession(missingSessionId)(FakeRequest("", ""))
        status(result) === NOT_FOUND
      }
    }

    "with valid session and authentication" should {

      "return 201" in new apiScope(clonedSession = Success(new ObjectId()),
        sessionAndItem = Success((Json.obj(), new PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))) {
        val result = api.cloneSession(new ObjectId().toString)(FakeRequest("", ""))
        status(result) === CREATED
      }

      "return apiClient" in new apiScope(clonedSession = Success(new ObjectId()),
        sessionAndItem = Success((Json.obj(), new PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))) {
        val result = api.cloneSession(new ObjectId().toString)(FakeRequest("", ""))
        (contentAsJson(result) \ "apiClient").asOpt[String] === Some(V2ActionsFactory.apiClient.clientId.toString)
      }

      "return cloned session options decryptable by apiClient" in new apiScope(
        clonedSession = Success(new ObjectId()),
        sessionAndItem = Success((Json.obj(), new PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))) {
        val encryptedData = "encrypted"
        mockEncryptionService.encrypt(any[ApiClient], any[String]) returns Some(EncryptionSuccess("clientId", encryptedData))
        val result = api.cloneSession(new ObjectId().toString)(FakeRequest("", ""))
        (contentAsJson(result) \ "options").as[String] === encryptedData
      }
    }
  }

  "V2 - ItemSessionApi" should {

    "when calling create" should {

      "fail when service fails" in new apiScope(Success(true)) {
        val result = api.create(VersionedId(ObjectId.get))(req.withBody(Some()))
        val json = Json.obj("message" -> "no session id returned from mock", "errorType" -> "errorSaving")
        result must beCodeAndJson(BAD_REQUEST, json)
      }

      "work" in new apiScope(
        Success(true),
        maybeSessionId = Some(ObjectId.get)) {
        val result = api.create(VersionedId(ObjectId.get))(FakeRequest("", "").withBody(Some()))
        result must beCodeAndJson(OK, Json.obj("id" -> maybeSessionId.get.toString))
      }

      "work with json header, but no body" in new apiScope(
        Success(true),
        maybeSessionId = Some(ObjectId.get)) {
        val result = api.create(VersionedId(ObjectId.get))(FakeRequest("", "")
          .withHeaders(("Content-Type", "application/json")).withBody(None))
        result must beCodeAndJson(OK, Json.obj("id" -> maybeSessionId.get.toString))
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
        result must beCodeAndJson(error.statusCode, error.json)
      }

      def emptyPlayerDefinition = PlayerDefinition(Seq.empty, "", Json.obj(), "", None)
      "fail when the session has no 'components'" in new apiScope(
        sessionAndItem = Success(Json.obj(), emptyPlayerDefinition)) {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = sessionDoesNotContainResponses("sessionId")
        result must beCodeAndJson(error.statusCode, error.json)
      }

      "work" in new apiScope(
        sessionAndItem = Success(
          Json.obj("components" -> Json.obj()),
          emptyPlayerDefinition),
        scoreResult = Success(Json.obj("score" -> 100))) {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        result must beCodeAndJson(OK, Json.obj("score" -> 100))
      }
    }
  }

  "orgCount" should {

    val orgId = new ObjectId()
    val month = "01-2016"

    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

    val results = {
      val r = scala.util.Random
      1.to(31).map(day => new DateTime().withMonthOfYear(1).withDayOfMonth(day) -> r.nextInt(100).toLong).toMap
    }

    "return OK" in new apiScope() {
      val result = api.orgCount(orgId, month)(req)
      status(result) must be equalTo (OK)
    }

    "return JSON formatted results from SessionAuth.orgCount" in new apiScope(
      sessionCounts = Success(results)) {
      val result = api.orgCount(orgId, month)(req)
      val format = DateTimeFormat.forPattern("MM/dd")
      contentAsJson(result) must be equalTo (
        JsArray(results.toSeq.sortBy(_._1).map { case (date, count) => Json.obj("date" -> format.print(date), "count" -> count) }))
    }

  }
}
