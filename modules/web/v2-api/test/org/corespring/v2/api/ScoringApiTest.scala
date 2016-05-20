package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.{ ApiClientEncryptionService, EncryptionSuccess }
import org.corespring.models.auth.ApiClient
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.services.{ OrgScoringService, ScoreResult }
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, RequestHeader }
import play.api.http.Status._
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

class ScoringApiTest extends Specification with Mockito with MockFactory {

  lazy val mockedOrgAndOpts = mockOrgAndOpts()
  private lazy val client: ApiClient = ApiClient(mockedOrgAndOpts.org.id, ObjectId.get, "secret")

  val rootOrgId = new ObjectId()
  val rootApiClient = ApiClient(rootOrgId, ObjectId.get, "secret")
  val rootOrgAndClient = Success((mockOrgAndOpts().copy(org = mockOrg().copy(id = rootOrgId)), rootApiClient))

  class apiScope(
    val orgAndClient: V2ApiScope.OrgAndClient = Success((mockedOrgAndOpts, client)),
    val sessionAndItem: Validation[V2Error, (JsValue, PlayerDefinition)] = Failure(generalError("error getting score")),
    val sessionAndItemMultiple: Seq[(String, Validation[V2Error, (JsValue, PlayerDefinition)])] = Seq(("sessionId", Failure(generalError("error getting score")))),
    val scoreResult: Validation[V2Error, JsValue] = Failure(generalError("error getting score"))) extends Scope {

    val mockEncryptionService = {
      val m = mock[ApiClientEncryptionService]
      m.encrypt(any[ApiClient], any[String]) returns Some(EncryptionSuccess("apiClient", "encrypted"))
      m
    }

    def getOrgAndClient(rh: RequestHeader) = orgAndClient

    def sessionCreatedForItem(id: VersionedId[ObjectId]): Unit = {}

    val apiContext = ScoringApiExecutionContext(ExecutionContext.Implicits.global, ExecutionContext.Implicits.global)

    lazy val orgScoringService = {
      val m = mock[OrgScoringService]
      m.scoreMultipleSessions(any[OrgAndOpts])(any[Seq[String]]) returns Future.successful(Seq(ScoreResult("sessionId", scoreResult)))
      m.scoreSession(any[OrgAndOpts])(any[String]) returns Future.successful(ScoreResult("sessionId", scoreResult))
      m
    }

    val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts] = { request: RequestHeader =>
      getOrgAndClient(request).map(_._1)
    }

    val api = new ScoringApi(
      apiContext,
      orgScoringService,
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

      "return a score error statusCode and json" in new apiScope(
        sessionAndItem = Success(Json.obj(), emptyPlayerDefinition)) {

        val err = generalError("score error")
        orgScoringService.scoreSession(any[OrgAndOpts])(any[String]) returns {
          Future.successful(ScoreResult("sessionId", Failure(err)))
        }

        val result = api.loadScore("sessionId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        result must beCodeAndJson(err.statusCode, err.json)
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
        val error = Json.obj("message" -> "No sessionIds found.", "errorType" -> "missingSessionIds")
        result must beCodeAndJson(BAD_REQUEST, error)
      }

      "return a scoreService error" in new apiScope(
        sessionAndItemMultiple = Seq(("sessionId", Success(
          Json.obj(),
          emptyPlayerDefinition))),
        scoreResult = Success(Json.obj("score" -> 100))) {

        val err = generalError("score error")

        orgScoringService.scoreMultipleSessions(any[OrgAndOpts])(any[Seq[String]]) returns {
          Future.successful(Seq(ScoreResult("sessionId", Failure(err))))
        }
        val result = api.loadMultipleScores()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("sessionIds" -> Json.arr("sessionId")))))
        val error = Json.arr(Json.obj("sessionId" -> "sessionId", "error" -> err.json))
        result must beCodeAndJson(OK, error)
      }

      "work" in new apiScope(
        sessionAndItemMultiple = Seq(("sessionId", Success(
          Json.obj("components" -> Json.obj()),
          emptyPlayerDefinition))),
        scoreResult = Success(Json.obj("score" -> 100))) {
        val result = api.loadMultipleScores()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("sessionIds" -> Json.arr("sessionId")))))
        val expectedResult = Json.arr(Json.obj("sessionId" -> "sessionId", "result" -> Json.obj("score" -> 100)))
        result must beCodeAndJson(OK, expectedResult)
      }
    }
  }
}
