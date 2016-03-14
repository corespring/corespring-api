package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.ApiClientEncryptionService
import org.corespring.models.Organization
import org.corespring.models.auth.ApiClient
import org.corespring.services.OrganizationService
import org.corespring.services.auth.ApiClientService
import org.corespring.v2.auth.identifiers.PlayerTokenIdentity.Keys
import org.corespring.v2.auth.models.{ OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors.{ incorrectJsonFormat, invalidQueryStringParameter, missingQueryStringParameter, noPlayerTokenInQueryString }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.warnings.Warnings.deprecatedQueryStringParameter
import org.specs2.execute.Result
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.{ Fragments, Scope }
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.test.FakeRequest

import scalaz.{ Failure, Success, Validation }

class PlayerTokenIdentityTest extends Specification with Mockito {

  lazy val mockOrg = {
    val m = mock[Organization]
    m.id returns ObjectId.get
    m
  }

  trait scope extends Scope {

    lazy val apiClient = ApiClient(mockOrg.id, ObjectId.get, "secret")

    lazy val orgService = {
      val m = mock[OrganizationService]
      m.findOneById(any[ObjectId]) returns Some(mockOrg)
      m
    }

    lazy val apiClientService = {
      val m = mock[ApiClientService]
      m.findByClientId(any[String]) returns Some(apiClient)
      m
    }

    lazy val apiClientEncryptionService = {
      val m = mock[ApiClientEncryptionService]
      m.decrypt(any[ApiClient], any[String]).answers((args: Any, _: Any) => {

        println(args)
        Some(args.asInstanceOf[Array[Any]](1).asInstanceOf[String])
      })
      m
    }

    lazy val identifier = new PlayerTokenIdentity(
      orgService,
      apiClientService,
      apiClientEncryptionService,
      PlayerTokenConfig(false))
  }

  "apply" should {

    "return an error for an empty path" in new scope {
      identifier.apply(FakeRequest("", "")).isFailure must_== true
    }

    s"return a bad param name error" in new scope {
      identifier.apply(FakeRequest("GET", "?apiClientId=blah")) must_== Failure(invalidQueryStringParameter("apiClientId", Keys.apiClient))
    }

    "return missingQueryStringParameter error for apiClient" in new scope {
      val req = FakeRequest("GET", "?hi=hi")
      identifier(req) must_== Failure(missingQueryStringParameter(Keys.apiClient))
    }

    "return no apiClientAndPlayerToken error" in new scope {
      val req = FakeRequest("GET", "?apiClient=bad")
      identifier(req) must_== Failure(noPlayerTokenInQueryString(req))
    }

    def assertApply(msg: String,
      json: JsValue = Json.toJson(PlayerAccessSettings.ANYTHING),
      clientKey: String = "apiClient",
      tokenKey: String = "playerToken")(assertFn: PartialFunction[Validation[V2Error, OrgAndOpts], Result]): Fragments = {

      msg in new scope {
        val jsonSettings = Json.stringify(json)
        val path = s"?$clientKey=1&$tokenKey=$jsonSettings"
        val request = FakeRequest("GET", path)
        val result = identifier.apply(request)
        if (assertFn.isDefinedAt(result)) {
          assertFn(result)
        } else {
          ko("failed")
        }
      }
    }

    assertApply(
      "return an error if the json can't be read as PlayerAccessSettings",
      Json.obj("itemId" -> "*")) {
        case Failure(err) =>
          println(s"Err: $err")
          val arr = (err.json \ "json-errors").as[JsArray]
          ((arr(0) \ "errors")(0) \ "message").as[String] must_== "Missing 'expires'"
          err.message must_== incorrectJsonFormat(Json.obj("itemId" -> "*")).message
      }

    assertApply("return a warning if 'options' is used as a queryString param",
      tokenKey = "options") {
        case Success(OrgAndOpts(_, _, _, _, _, warnings)) =>
          warnings(0) === deprecatedQueryStringParameter("options", "playerToken")
      }

    assertApply("return success with no warnings") {
      case Success(OrgAndOpts(org, _, _, _, _, warnings)) =>
        warnings.length === 0
        org === mockOrg
    }

    assertApply("returns success with no warnings for editorToken", tokenKey = "editorToken") {
      case Success(OrgAndOpts(org, _, _, _, _, warnings)) =>
        warnings.length === 0
        org === mockOrg
    }
  }
}
