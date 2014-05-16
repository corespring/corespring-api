package org.corespring.platform.core.controllers.auth

import org.specs2.mutable.Specification
import play.api.test.{ PlaySpecification, FakeApplication, FakeRequest }

class TokenReaderTest extends Specification with PlaySpecification {

  "TokenReader" should {

    val reader = new TokenReader {}

    def assertToken[A](r: FakeRequest[A], expected: Either[String, String]) = {
      reader.getToken(r, "Invalid token", "No token") === expected
    }

    def fakeApp = FakeApplication(
      additionalConfiguration = Map("application.secret" -> "test"))

    "read - no token" in {
      val request = FakeRequest()
      assertToken(request, Left("No token"))
    }

    "read - query token" in {
      val request = FakeRequest("", s"?${TokenReader.AccessToken}=token")
      assertToken(request, Right("token"))
    }

    "read - session token" in {
      running(fakeApp) {
        val request = FakeRequest("", "")
          .withSession(TokenReader.AccessToken -> "session-token")
        assertToken(request, Right("session-token"))
      }
    }

    "read - header token" in {
      val request = FakeRequest("", "")
        .withHeaders(TokenReader.AuthorizationHeader -> s"${TokenReader.Bearer}- header-token")
      assertToken(request, Left("Invalid token"))
    }

    "read - header token error" in {
      val request = FakeRequest("", "")
        .withHeaders(TokenReader.AuthorizationHeader -> s"${TokenReader.Bearer} header-token")
      assertToken(request, Right("header-token"))
    }
  }
}
