package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.encryption.{ EncryptionFailure, EncryptionResult, EncryptionSuccess, OrgEncrypter }
import org.corespring.v2.api.services.{ CreateTokenResult, PlayerTokenService }
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors.{ missingRequiredField, encryptionFailed, generalError, noJson }
import org.corespring.v2.errors.{ Field, V2Error }
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
    val createTokenResult: Validation[V2Error, CreateTokenResult] = Failure(generalError("Create token failure")),
    val orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Failure(generalError("Test V2 Error"))) extends Scope {
    lazy val api = new PlayerTokenApi {

      override def tokenService: PlayerTokenService = {
        val m = mock[PlayerTokenService]
        m.createToken(any[ObjectId], any[JsValue]) returns createTokenResult
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = {
        orgAndOptsResult
      }
    }
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

      /* Move to PlayerTokenServiceTest
      "fail to create if missing 'expires' in json request body" in new playerScope(
        orgAndOptsResult = Success(OrgAndOpts(mockOrgId, PlayerAccessSettings.ANYTHING, AuthMode.ClientIdAndPlayerToken))) {
        val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        status(result) === BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] ==== missingRequiredField(Field("expires", "number")).message
      }*/

      "fail to create if create token fails" in new playerScope(
        orgAndOptsResult = Success(OrgAndOpts(mockOrgId, PlayerAccessSettings.ANYTHING, AuthMode.ClientIdAndPlayerToken))) {
        val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("expires" -> 0))))
        val error = createTokenResult.toEither.left.get
        status(result) === error.statusCode
        (contentAsJson(result) \ "message").as[String] === error.message
      }

      /*
       Move to PlayerTokenServiceTest
      "fail to create if encrypter returns failure" in new playerScope(
        orgAndOptsResult = Success(OrgAndOpts(mockOrgId, PlayerAccessSettings.ANYTHING, AuthMode.ClientIdAndPlayerToken)),
        createTokenResult = Failure(generalError("?"))) {
        val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("expires" -> 0))))
        val error = encryptionFailed("A Failure")
        status(result) === error.statusCode
        (contentAsJson(result) \ "message").as[String] === error.message
      }*/
    }
    "with a valid request" should {

      class withJsonPlayerScope(json: JsValue) extends playerScope(
        orgAndOptsResult = Success(OrgAndOpts(mockOrgId, PlayerAccessSettings.ANYTHING, AuthMode.ClientIdAndPlayerToken)),
        createTokenResult = Success(CreateTokenResult("clientid", "encrypted", Json.obj("test-success" -> true)))) {
        lazy val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(json)))
        status(result) === OK
        lazy val jsonResult = (contentAsJson(result))
      }

      "work" in new withJsonPlayerScope(Json.obj("expires" -> 0)) {
        println(jsonResult)
        (jsonResult \ "playerToken").as[String] === "encrypted"
        (jsonResult \ "apiClient").as[String] === "clientid"
        (jsonResult \ "accessSettings").as[JsObject] === Json.obj("test-success" -> true)
      }
    }
    /*
       Move to PlayerTokenServiceTest
    "with a valid request" should {
      "ignore json properties that arent part of access settings and default to wildcards" in new withJsonPlayerScope(Json.obj("expires" -> 0)) {
        (jsonResult \ "playerToken").as[String] === "encrypted"
        (jsonResult \ "apiClient").as[String] === "clientid"
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "*", "sessionId" -> "*", "secure" -> false, "expires" -> "0", "mode" -> "*")
      }

      "pass in itemId" in new withJsonPlayerScope(Json.obj("expires" -> "0", "itemId" -> "itemId")) {
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "itemId", "sessionId" -> "*", "secure" -> false, "expires" -> "0", "mode" -> "*")
      }

      "pass in itemId, sessionId" in new withJsonPlayerScope(
        Json.obj("expires" -> 0, "itemId" -> "itemId", "sessionId" -> "sessionId")) {
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "secure" -> false, "expires" -> "0", "mode" -> "*")
      }

      /**
       * Note: "mode" is under review to see if it's a valid option.
       * @see SimpleWildcardChecker
       */

      "pass in itemId, sessionId, expires" in new withJsonPlayerScope(
        Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "expires" -> "19")) {
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "secure" -> false, "expires" -> "19", "mode" -> "*")
      }

      "pass in itemId, sessionId, expires and secure" in new withJsonPlayerScope(
        Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "expires" -> "19", "secure" -> true)) {
        (jsonResult \ "accessSettings").as[JsObject] ===
          Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "secure" -> true, "expires" -> "19", "mode" -> "*")
      }

    }
     */
  }
}
