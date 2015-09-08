package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.{ ApiClientEncryptionService, EncryptionFailure, EncryptionSuccess, EncryptionResult }
import org.corespring.models.auth.ApiClient
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.{ missingRequiredField, encryptionFailed }
import org.corespring.v2.errors.Field
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }

import scalaz.{ Failure, Success }

class PlayerTokenServiceTest extends Specification with Mockito {

  class serviceScope(encryptionResult: EncryptionResult = EncryptionSuccess("clientId", "data")) extends Scope {

    val encryptionService: ApiClientEncryptionService = {
      val m = mock[ApiClientEncryptionService]
      m.encryptByOrg(any[ObjectId], anyString) returns Some(encryptionResult)
      m
    }

    val service = new PlayerTokenService(encryptionService)
  }

  class serviceScopeWithJsonBack(jsonIn: JsValue) extends serviceScope {
    lazy val result = service.createToken(ObjectId.get, jsonIn)
    lazy val jsonBack = result match {
      case Success(CreateTokenResult(_, _, jsonBack)) => jsonBack
      case _ => Json.obj("error" -> "should't get this json")
    }
  }

  "PlayerTokenService" should {

    "fail if encryption fails" in new serviceScope(
      EncryptionFailure("?", new RuntimeException("?"))) {
      val result = service.createToken(ObjectId.get, Json.obj("expires" -> 0))
      result must_== Failure(encryptionFailed("?"))
    }

    "fail if expires is missing" in new serviceScope() {
      val result = service.createToken(ObjectId.get, Json.obj())
      result must_== Failure(missingRequiredField(Field("expires", "number")))
    }

    "create token" in new serviceScope() {
      val result = service.createToken(ObjectId.get, Json.obj("expires" -> 0))
      val accessSettings = PlayerAccessSettings.ANYTHING.copy(mode = Some(PlayerAccessSettings.STAR))
      result must_== Success(CreateTokenResult("clientId", "data", Json.toJson(accessSettings)))
    }

    "create token with itemId" in new serviceScopeWithJsonBack(Json.obj("expires" -> "0", "itemId" -> "itemId")) {
      jsonBack must_== Json.obj("itemId" -> "itemId", "sessionId" -> "*", "secure" -> false, "expires" -> "0", "mode" -> "*")
    }

    "create token with itemId, sessionId" in new serviceScopeWithJsonBack(
      Json.obj("expires" -> 0, "itemId" -> "itemId", "sessionId" -> "sessionId")) {
      jsonBack must_==
        Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "secure" -> false, "expires" -> "0", "mode" -> "*")
    }

    "create token with itemId, sessionId, expires" in new serviceScopeWithJsonBack(
      Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "expires" -> "19")) {
      jsonBack must_==
        Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "secure" -> false, "expires" -> "19", "mode" -> "*")
    }

    "create token with itemId, sessionId, expires and secure" in new serviceScopeWithJsonBack(
      Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "expires" -> "19", "secure" -> true)) {
      jsonBack must_==
        Json.obj("itemId" -> "itemId", "sessionId" -> "sessionId", "secure" -> true, "expires" -> "19", "mode" -> "*")
    }
  }
}
