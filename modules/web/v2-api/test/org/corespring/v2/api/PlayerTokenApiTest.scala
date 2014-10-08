package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.encryption.{ EncryptionSuccess, EncryptionFailure, EncryptionResult, OrgEncrypter }
import org.corespring.v2.auth.models.{ AuthMode, PlayerAccessSettings, OrgAndOpts }
import org.corespring.v2.errors.Errors.{ encryptionFailed, generalError, noJson }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.mvc.{ AnyContentAsJson, RequestHeader }
import play.api.test.{ FakeHeaders, FakeRequest, PlaySpecification }

import scala.concurrent.ExecutionContext
import scalaz.{ Success, Failure, Validation }

class PlayerTokenApiTest extends Specification
  with Mockito with PlaySpecification {

  val mockOrgId = ObjectId.get

  class playerScope(
    encryptionResult: Option[EncryptionResult] = None,
    orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Failure(generalError("Test V2 Error"))) extends Scope {

    lazy val api = new PlayerTokenApi {
      override def encrypter: OrgEncrypter = {
        val m = mock[OrgEncrypter]
        m.encrypt(any[ObjectId], anyString) returns encryptionResult
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = {
        orgAndOptsResult
      }
    }
  }

  "PlayerTokenApi" should {

    "fail to create if there is no json in the request body" in new playerScope {
      val result = api.createPlayerToken()(FakeRequest("", ""))
      status(result) === BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] ==== noJson.message
    }

    "fail to create if orgId and opts can't be found" in new playerScope {
      val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("a" -> "b"))))
      status(result) === BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] ==== "Test V2 Error"
    }

    "fail to create if encrypter returns None" in new playerScope(
      orgAndOptsResult = Success(OrgAndOpts(mockOrgId, PlayerAccessSettings.ANYTHING, AuthMode.ClientIdAndPlayerToken))) {
      val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("a" -> "b"))))
      val error = encryptionFailed(s"orgId: $mockOrgId - Unknown error trying to encrypt")
      status(result) === error.statusCode
      (contentAsJson(result) \ "message").as[String] === error.message
    }

    "fail to create if encrypter returns failure" in new playerScope(
      orgAndOptsResult = Success(OrgAndOpts(mockOrgId, PlayerAccessSettings.ANYTHING, AuthMode.ClientIdAndPlayerToken)),
      encryptionResult = Some(EncryptionFailure("A Failure", new RuntimeException("?")))) {
      val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("a" -> "b"))))
      val error = encryptionFailed("A Failure")
      status(result) === error.statusCode
      (contentAsJson(result) \ "message").as[String] === error.message
    }

    "succeeds" in new playerScope(
      orgAndOptsResult = Success(OrgAndOpts(mockOrgId, PlayerAccessSettings.ANYTHING, AuthMode.ClientIdAndPlayerToken)),
      encryptionResult = Some(EncryptionSuccess("clientid", "encrypted", None))) {
      val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("a" -> "b"))))
      val error = encryptionFailed("A Failure")
      status(result) === OK
      (contentAsJson(result) \ "playerToken").as[String] === "encrypted"
    }
  }
}
