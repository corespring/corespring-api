package developer.controllers

import org.bson.types.ObjectId
import org.corespring.errors.GeneralError
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ OrganizationHelper, ApiClientHelper }
import org.corespring.web.api.v1.errors.ApiError
import org.specs2.mutable.After
import org.specs2.specification.Scope
import developer.controllers.routes.{ AuthController => Route }
import play.api.libs.json.Json
import play.api.test.FakeRequest

class AuthControllerIntegrationTest extends IntegrationSpecification {

  trait scope extends Scope with After {

    val orgId = OrganizationHelper.create(this.getClass.getSimpleName)
    val apiClient = ApiClientHelper.create(orgId)

    override def after = removeData()
  }

  "getAccessToken" should {

    trait accessToken extends scope {

      def clientId: String = apiClient.clientId.toString
      def clientSecret: String = apiClient.clientSecret.toString

      lazy val call = Route.getAccessToken()
      lazy val request = FakeRequest(call.method, call.url)
        .withFormUrlEncodedBody(
          OAuthConstants.ClientId -> clientId,
          OAuthConstants.ClientSecret -> clientSecret)
      lazy val result = route(request).get
    }

    trait okAccessToken extends accessToken

    trait badClientId extends accessToken {
      override def clientId = ObjectId.get.toString
    }

    trait badClientSecret extends accessToken {
      override def clientSecret = ObjectId.get.toString
    }

    s"return $OK for a valid request" in new okAccessToken {
      status(result) must_== OK
    }

    s"return json with the new token in it" in new okAccessToken {
      val json = contentAsJson(result)
      val token = main.tokenService.find(orgId, None).get
      json must_== Json.obj(OAuthConstants.AccessToken -> token.tokenId)
    }

    s"return $FORBIDDEN for bad client id " in new badClientId {
      status(result) must_== FORBIDDEN
    }

    s"return error json for bad client id " in new badClientId {
      val json = contentAsJson(result)
      (json \ "message").asOpt[String] must_== Some("No api client found")
    }

    s"return $FORBIDDEN for bad client secret " in new badClientSecret {
      status(result) must_== FORBIDDEN
    }

    s"return error json for bad client secret " in new badClientSecret {
      val json = contentAsJson(result)
      (json \ "message").asOpt[String] must_== Some("No api client found")
    }
  }
}
