package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.v2.api.services.{ CreateTokenResult, PlayerTokenService }
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.{ generalError, noJson }
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
  with Mockito with PlaySpecification with MockFactory {

  val org = mockOrg()

  class playerScope(
    val createTokenResult: Validation[V2Error, CreateTokenResult] = Failure(generalError("Create token failure")),
    val orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Failure(generalError("Test V2 Error"))) extends Scope {

    val tokenService = {
      val m = mock[PlayerTokenService]
      m.createToken(any[ObjectId], any[JsValue]) returns createTokenResult
      m
    }

    def getOrgAndOpts(request: RequestHeader): Validation[V2Error, OrgAndOpts] = {
      orgAndOptsResult
    }

    val api = new PlayerTokenApi(tokenService, V2ApiExecutionContext(ExecutionContext.global), getOrgAndOpts)
  }

  "PlayerTokenApi" should {

    "with an invalid request" should {
      "fail to create if orgId and opts can't be found" in new playerScope {
        val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("a" -> "b"))))
        result must beCodeAndJson(BAD_REQUEST, orgAndOptsResult.swap.toOption.get.json)
      }

      "fail to create if there is no json in the request body" in new playerScope(
        orgAndOptsResult = Success(mockOrgAndOpts(AuthMode.ClientIdAndPlayerToken))) {
        val result = api.createPlayerToken()(FakeRequest("", ""))
        result must beCodeAndJson(noJson.statusCode, noJson.json)
      }

      "fail to create if create token fails" in new playerScope(
        orgAndOptsResult = Success(mockOrgAndOpts(AuthMode.ClientIdAndPlayerToken))) {
        val result = api.createPlayerToken()(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj("expires" -> 0))))
        val error = createTokenResult.toEither.left.get
        result must beCodeAndJson(error.statusCode, error.json)
      }
    }

    "with a valid request" should {

      class withJsonPlayerScope(json: JsValue) extends playerScope(
        orgAndOptsResult = Success(mockOrgAndOpts(AuthMode.ClientIdAndPlayerToken)),
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
  }
}
