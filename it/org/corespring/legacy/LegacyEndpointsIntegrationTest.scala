package org.corespring.legacy

import org.corespring.it.IntegrationSpecification
import org.specs2.specification.Scope
import play.api.http.HeaderNames
import play.api.test.FakeRequest

class LegacyEndpointsIntegrationTest extends IntegrationSpecification {

  "encryptionOptions" should {

    trait encryptOptions extends Scope {
      val call = org.corespring.legacy.routes.LegacyEndpoints.encryptOptions()
      val request = FakeRequest(call.method, call.url)
      lazy val result = route(request).get
    }

    s"return $MOVED_PERMANENTLY" in new encryptOptions {
      status(result) must_== MOVED_PERMANENTLY
    }

    s"return ${HeaderNames.LOCATION}: /api/v2/player-token" in new encryptOptions {
      header(HeaderNames.LOCATION, result) must_== Some("/api/v2/player-token")
    }
  }
}
