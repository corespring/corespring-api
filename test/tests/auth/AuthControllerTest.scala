package tests.auth

import tests.BaseTest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.Play.current
import play.api.Play
import securesocial.core.SecureSocial

class AuthControllerTest extends BaseTest{

  "cannot register organization for client id and secret without logging in through SS" in {
//    val fakeRequest = FakeRequest(GET, "/auth/register")
//    val result = routeAndCall(fakeRequest).get
//    status(result) must not equalTo(OK)
  }

  "cannot register organization for client id and secret when logged in through SS without write permission to organization" in {
//    val fakeRequest = FakeRequest(GET, "/auth/register").withSession(
//      (SecureSocial.UserKey -> "homer"),
//      (SecureSocial.ProviderKey -> "userpass")
//    )
//    val result = routeAndCall(fakeRequest).get
//    status(result) must not equalTo(OK)
  }

  "can register organization for client id and secret when logged in through SS and with write permission to organization" in {
    pending
  }

  "re-registering organization results in same client id and secret" in {
    pending
  }

  "can retrieve auth token using client id and secret" in {
    pending
  }

  "can use auth token to retrieve list of organizations" in {
   pending
  }
}
