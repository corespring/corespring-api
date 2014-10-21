package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.v2.auth.models.{ PlayerAccessSettings, AuthMode, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.mvc.{ AnyContentAsJson, AnyContentAsEmpty, RequestHeader }
import play.api.test.{ PlaySpecification, FakeHeaders, FakeRequest }

import scala.concurrent.ExecutionContext
import scalaz.{ Success, Validation }

class ExternalModelLaunchApiTest
  extends Specification
  with PlaySpecification
  with Mockito {

  class apiScope(
    orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(OrgAndOpts(ObjectId.get, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken))) extends Scope {

    lazy val api = new ExternalModelLaunchApi {
      override def sessionService: V2SessionService = {
        val m = mock[V2SessionService]
        m
      }

      override def tokenService: PlayerTokenService = {
        val m = mock[PlayerTokenService]
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

    }
  }

  "ExternalModelApi" should {
    "launch" in new apiScope {
      val json = AnyContentAsJson(Json.obj(
        "accessSettings" -> Json.obj("expires" -> 0),
        "model" -> Json.obj()))

      val result = api.buildExternalLaunchSession()(FakeRequest("", "", FakeHeaders(), json))
      status(result) === OK
    }
  }
}