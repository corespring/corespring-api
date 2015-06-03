package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.platform.core.encryption.ApiClientEncrypter
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.core.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, RequestHeader }
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class ItemSessionApiTest extends Specification with Mockito with MockFactory {

  class apiScope(
      val canCreate: Validation[V2Error, Boolean] = Failure(generalError("no")),
      val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts()),
      val maybeSessionId: Option[ObjectId] = None,
      val sessionAndItem: Validation[V2Error, (JsValue, PlayerDefinition)] = Failure(generalError("no")),
      val scoreResult: Validation[V2Error, JsValue] = Failure(generalError("error getting score")),
      val clonedSession: Validation[V2Error, ObjectId] = Failure(generalError("no")),
      val apiClient: Option[ApiClient] = None) extends Scope {

    val api: ItemSessionApi = new ItemSessionApi {
      override def randomApiClient(orgId: ObjectId): Option[ApiClient] = apiClient
      override def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = {
        val m = mock[SessionAuth[OrgAndOpts, PlayerDefinition]]
        m.canCreate(anyString)(any[OrgAndOpts]) returns canCreate
        m.loadForRead(anyString)(any[OrgAndOpts]) returns sessionAndItem
        m.loadForWrite(anyString)(any[OrgAndOpts]) returns sessionAndItem
        m.loadWithIdentity(anyString)(any[OrgAndOpts]) returns sessionAndItem
        m.cloneIntoPreview(anyString)(any[OrgAndOpts]) returns clonedSession
        import scalaz.Scalaz._
        m.create(any[JsValue])(any[OrgAndOpts]) returns maybeSessionId.toSuccess(errorSaving("no session id returned from mock"))
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

      override def orgService: OrgService = ???
    }
  }

  "cloneSession" should {

    PlaySingleton.start()

    "with invalid session" should {
      val missingSessionId = new ObjectId().toString

      "return 404" in new apiScope(clonedSession = Failure(cantLoadSession(missingSessionId))) {
        val result = api.cloneSession(missingSessionId)(FakeRequest("",""))
        status(result) must be equalTo(NOT_FOUND)
      }

    }

    "without authentication" should {

      val sessionId = new ObjectId().toString
      val request = FakeRequest("","")

      "return 401" in new apiScope(clonedSession = Success(new ObjectId()),
        orgAndOpts = Failure(noOrgIdAndOptions(request))) {
        val result = api.cloneSession(sessionId)(request)
        status(result) must be equalTo(UNAUTHORIZED)
      }

    }

    "with valid session and authentication" should {

      val apiClient = ApiClient(mockOrg().id, new ObjectId(), "secret")

      "return 201" in new apiScope(clonedSession = Success(new ObjectId()), apiClient = Some(apiClient),
        sessionAndItem = Success((Json.obj(), new PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))) {
        val result = api.cloneSession(new ObjectId().toString)(FakeRequest("", ""))
        status(result) must be equalTo(CREATED)
      }

      "return apiClient" in new apiScope(clonedSession = Success(new ObjectId()), apiClient = Some(apiClient),
        sessionAndItem = Success((Json.obj(), new PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))) {
        val result = api.cloneSession(new ObjectId().toString)(FakeRequest("", ""))
        (contentAsJson(result) \ "apiClient").as[String] must be equalTo(apiClient.clientId.toString)
      }

      "return cloned session options decryptable by apiClient" in new apiScope(
        clonedSession = Success(new ObjectId()), apiClient = Some(apiClient),
        sessionAndItem = Success((Json.obj(), new PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))) {
        val result = api.cloneSession(new ObjectId().toString)(FakeRequest("", ""))
        val encrypter = new ApiClientEncrypter(AESCrypto)

        encrypter.decrypt(apiClient, (contentAsJson(result) \ "options").as[String]) must be equalTo(
          Some(ItemSessionApi.clonedSessionOptions.toString))
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