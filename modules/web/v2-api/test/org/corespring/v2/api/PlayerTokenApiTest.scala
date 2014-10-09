package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.encryption.{ EncryptionFailure, EncryptionResult, EncryptionSuccess, OrgEncrypter }
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors.{ encryptionFailed, generalError, noJson }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, RequestHeader }
import play.api.test.{ FakeHeaders, FakeRequest, PlaySpecification }

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class PlayerTokenApiTest extends Specification
  with Mockito with PlaySpecification {

  val mockOrgId = ObjectId.get

  class playerScope(
    val encryptionResult: Option[EncryptionResult] = None,
    val orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Failure(generalError("Test V2 Error"))) extends Scope {
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

  class withJsonPlayerScope(json: JsValue) extends playerScope(
    orgAndOptsResult = Success(OrgAndOpts(mockOrgId, PlayerAccessSettings.ANYTHING, AuthMode.ClientIdAndPlayerToken)),
    encryptionResult = Some(EncryptionSuccess("clientid", "encrypted", None))) {
    lazy val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(json)))
    status(result) === OK
    lazy val jsonResult = (contentAsJson(result))
  }

  "PlayerTokenApi" should {

    "with an invalid request" should {
      "fail to create if orgId and opts can't be found" in new playerScope {
        val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("a" -> "b"))))
        status(result) === BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] ==== "Test V2 Error"
      }

      "fail to create if there is no json in the request body" in new playerScope(
        orgAndOptsResult = Success(OrgAndOpts(mockOrgId, PlayerAccessSettings.ANYTHING, AuthMode.ClientIdAndPlayerToken))) {
        val result = api.createPlayerToken()(FakeRequest("", ""))
        status(result) === BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] ==== noJson.message
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
    }

    "with a valid request" should {
      "ignore json properties that arent part of access settings and default to wildcards" in new withJsonPlayerScope(Json.obj("a" -> "b")) {
        (jsonResult \ "playerToken").as[String] === "encrypted"
        (jsonResult \ "apiClient").as[String] === "clientid"
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "*", "sessionId" -> "*", "secure" -> false, "mode" -> "*")
      }

      "pass in itemId" in new withJsonPlayerScope(Json.obj("itemId" -> "itemId")) {
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "itemId", "sessionId" -> "*", "secure" -> false, "mode" -> "*")
      }

      "pass in itemId, sessionId" in new withJsonPlayerScope(
        Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId")) {
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "secure" -> false, "mode" -> "*")
      }

      /**
       * Note: "mode" is under review to see if it's a valid option.
       * @see SimpleWildcardChecker
       */

      "pass in itemId, sessionId, expires" in new withJsonPlayerScope(
        Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "expires" -> 19)) {
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "secure" -> false, "expires" -> 19, "mode" -> "*")
      }

      "pass in itemId, sessionId, expires and secure" in new withJsonPlayerScope(
        Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "expires" -> 19, "secure" -> true)) {
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "secure" -> true, "expires" -> 19, "mode" -> "*")
      }

    }
  }
}
