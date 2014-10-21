package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.v2.api.services.{ CreateTokenResult, PlayerTokenService }
import org.corespring.v2.auth.models.{ PlayerAccessSettings, AuthMode, OrgAndOpts }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, JsObject, Json }
import play.api.mvc.{ AnyContentAsJson, AnyContentAsEmpty, RequestHeader }
import play.api.test.{ PlaySpecification, FakeHeaders, FakeRequest }

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class ExternalModelLaunchApiTest
  extends Specification
  with PlaySpecification
  with Mockito {

  case class apiScope(
    orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(OrgAndOpts(ObjectId.get, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken)),
    createSession: Option[ObjectId] = Some(ObjectId.get),
    createTokenResult: Validation[V2Error, CreateTokenResult] = Success(CreateTokenResult("apiClient", "token", Json.obj()))) extends Scope {

    lazy val api = new ExternalModelLaunchApi {
      override def sessionService: V2SessionService = {
        val m = mock[V2SessionService]
        m.createExternalModelSession(any[ObjectId], any[JsObject]) returns createSession
        m
      }

      override def tokenService: PlayerTokenService = {
        val m = mock[PlayerTokenService]
        m.createToken(any[ObjectId], any[JsValue]) returns createTokenResult
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

    }
  }

  "ExternalModelApi" should {

    "fail if a sessionId other than * is specified" in new apiScope() {
      val json = AnyContentAsJson(Json.obj(
        "accessSettings" -> Json.obj("expires" -> 0, "sessionId" -> "bad id"),
        "model" -> Json.obj()))
      val result = api.buildExternalLaunchSession()(FakeRequest("", "", FakeHeaders(), json))
      val error = api.badSessionIdError
      status(result) === error.statusCode
      contentAsJson(result) === error.json
    }

    "fail if create token fails" in new apiScope(
      createTokenResult = Failure(generalError("create-token-failed"))) {
      val json = AnyContentAsJson(Json.obj(
        "accessSettings" -> Json.obj("expires" -> 0),
        "model" -> Json.obj()))

      val result = api.buildExternalLaunchSession()(FakeRequest("", "", FakeHeaders(), json))
      val error = createTokenResult.toEither.left.get
      status(result) === error.statusCode
      contentAsJson(result) === error.json
    }

    "launch" in new apiScope {
      val json = AnyContentAsJson(Json.obj(
        "accessSettings" -> Json.obj("expires" -> 0),
        "model" -> Json.obj()))

      val result = api.buildExternalLaunchSession()(FakeRequest("", "", FakeHeaders(), json))
      status(result) === OK
    }
  }
}