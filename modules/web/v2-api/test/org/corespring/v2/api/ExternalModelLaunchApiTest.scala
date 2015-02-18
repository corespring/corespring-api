package org.corespring.v2.api

import scala.concurrent.ExecutionContext

import org.bson.types.ObjectId
import org.corespring.v2.api.services.{ CreateTokenResult, PlayerTokenService }
import org.corespring.v2.auth.models.{ MockFactory, AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.{ Field, V2Error }
import org.corespring.v2.errors.Errors.{ generalError, missingRequiredField, noJson }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, RequestHeader }
import play.api.test.{ FakeHeaders, FakeRequest, PlaySpecification }
import scalaz.{ Failure, Success, Validation }

class ExternalModelLaunchApiTest
  extends Specification
  with PlaySpecification
  with Mockito
  with MockFactory {

  case class apiScope(
    orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts()),
    createSession: Option[ObjectId] = Some(ObjectId.get),
    createTokenResult: Validation[V2Error, CreateTokenResult] = Success(CreateTokenResult("apiClient", "token", Json.obj())),
    expectedError: Option[V2Error] = None) extends Scope {

    lazy val api = new ExternalModelLaunchApi {
      override def sessionService: V2SessionService = {
        val m = mock[V2SessionService]
        m.createExternalModelSession(any[OrgAndOpts], any[JsObject]) returns createSession
        m
      }

      override def tokenService: PlayerTokenService = {
        val m = mock[PlayerTokenService]
        m.createToken(any[ObjectId], any[JsValue]) returns createTokenResult
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

      override def playerJsUrl: String = "/v2/player/player.js"
    }

    lazy val json = createJson
    lazy val result = api.buildExternalLaunchSession()(fakeRequest(json))
  }

  def createJson = Json.obj(
    "accessSettings" -> Json.obj("expires" -> 0),
    "model" -> Json.obj())

  def fakeRequest(json: JsValue) = FakeRequest("", "", FakeHeaders(), AnyContentAsJson(json))

  "ExternalModelApi" should {

    "fail if a sessionId other than * is specified" in new apiScope() {
      override lazy val json = createJson ++ Json.obj(
        "accessSettings" -> Json.obj(
          "expires" -> 0, "sessionId" -> "bad id"))
      val error = api.badSessionIdError
      status(result) === error.statusCode
      contentAsJson(result) === error.json
    }

    "fail if there is no json" in new apiScope() {
      override lazy val result = api.buildExternalLaunchSession()(FakeRequest("", ""))
      val error = noJson
      status(result) === error.statusCode
      contentAsJson(result) === error.json
    }

    "fail if 'model' missing" in new apiScope() {
      override lazy val json = Json.obj()
      val error = missingRequiredField(Field("model", "object"))
      status(result) === error.statusCode
      contentAsJson(result) === error.json
    }

    "fail if 'accessSettings' missing" in new apiScope() {
      override lazy val json = Json.obj("model" -> Json.obj())
      val error = missingRequiredField(Field("accessSettings", "object"))
      status(result) === error.statusCode
      contentAsJson(result) === error.json
    }

    "fail if create session fails" in new apiScope(
      createSession = None) {
      val error = api.createSessionError
      status(result) === error.statusCode
      contentAsJson(result) === error.json
    }

    "fail if create token fails" in new apiScope(
      createTokenResult = Failure(generalError("create-token-failed"))) {
      val error = createTokenResult.toEither.left.get
      status(result) === error.statusCode
      contentAsJson(result) === error.json
    }

    "launch" in new apiScope {
      status(result) === OK
    }
  }
}