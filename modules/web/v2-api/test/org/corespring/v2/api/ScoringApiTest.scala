package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.{ApiClientEncryptionService, EncryptionSuccess}
import org.corespring.models.auth.ApiClient
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrganizationService
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.{MockFactory, OrgAndOpts}
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{AnyContentAsJson, RequestHeader}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}

import scala.concurrent.ExecutionContext
import scalaz.{Failure, Success, Validation}

class ScoringApiTest extends Specification with Mockito with MockFactory {

  lazy val mockedOrgAndOpts = mockOrgAndOpts()
  private lazy val client: ApiClient = ApiClient(mockedOrgAndOpts.org.id, ObjectId.get, "secret")

  val rootOrgId = new ObjectId()
  val rootApiClient = ApiClient(rootOrgId, ObjectId.get, "secret")
  val rootOrgAndClient = Success((mockOrgAndOpts().copy(org = mockOrg().copy(id = rootOrgId)) , rootApiClient))

  class apiScope(
    val canCreate: Validation[V2Error, Boolean] = Failure(generalError("no")),
    val orgAndClient: V2ApiScope.OrgAndClient = Success((mockedOrgAndOpts, client)),
    val maybeSessionId: Option[ObjectId] = None,
    val sessionAndItem: Validation[V2Error, (JsValue, PlayerDefinition)] = Failure(generalError("no")),
    val scoreResult: Validation[V2Error, JsValue] = Failure(generalError("error getting score")),
    val clonedSession: Validation[V2Error, ObjectId] = Failure(generalError("no")),
    val apiClient: Option[ApiClient] = None,
    val sessionCounts: Validation[V2Error, Map[DateTime, Long]] = Success(Map.empty[DateTime, Long])) extends Scope {

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

    def getOrgAndClient(rh: RequestHeader) = orgAndClient

    def sessionCreatedForItem(id: VersionedId[ObjectId]): Unit = {}

    val apiContext = ScoringApiExecutionContext(ExecutionContext.Implicits.global)

    val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts] = { request: RequestHeader =>
      getOrgAndClient(request).map(_._1)
    }


    val api = new ScoringApi(
      mockSessionAuth,
      mockScoreService,
      apiContext,
      getOrgAndClient,
      getOrgAndOptionsFn)
  }


  "V2 - ScoringApi" should {

    def emptyPlayerDefinition = PlayerDefinition(Seq.empty, "", Json.obj(), "", None)

    "when calling loadScore" should {

      "fail when session and item are not found" in new apiScope() {
        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = sessionAndItem.toEither.left.get
        result must beCodeAndJson(error.statusCode, error.json)
      }

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

    "when calling loadMultipleScores" should {

      "fail when sessionIds are not found" in new apiScope() {
        val result = api.loadMultipleScores()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = Json.obj("message"->"No sessionIds found.","errorType"->"missingSessionIds")
        result must beCodeAndJson(BAD_REQUEST, error)
      }

      "fail when the session has no 'components'" in new apiScope(
        sessionAndItem = Success(Json.obj(), emptyPlayerDefinition)) {
        val result = api.loadMultipleScores()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("sessionIds" -> Json.arr("sessionId")))))
        val error = Json.arr(Json.obj("sessionId" -> "sessionId", "error" -> sessionDoesNotContainResponses("sessionId").json))
        result must beCodeAndJson(OK, error)
      }

      "work" in new apiScope(
        sessionAndItem = Success(
          Json.obj("components" -> Json.obj()),
          emptyPlayerDefinition),
        scoreResult = Success(Json.obj("score" -> 100))) {
        val result = api.loadMultipleScores()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("sessionIds" -> Json.arr("sessionId")))))
        val expectedResult = Json.arr(Json.obj("sessionId" -> "sessionId", "result" -> Json.obj("score" -> 100)))
        result must beCodeAndJson(OK, expectedResult)
      }
    }
  }
}
