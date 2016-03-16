package org.corespring.v2

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.AccessTokenHelper
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessTokenAndItem }
import org.specs2.specification.Scope

class AccessTokenIntegrationTest extends IntegrationSpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "access token" should {

    "ItemApi.get" should {

      trait scope
        extends Scope
        with orgWithAccessTokenAndItem
        with TokenRequestBuilder {
      }

      s"return $BAD_REQUEST - for an access token that has expired" in new scope {
        AccessTokenHelper.expire(accessToken)
        lazy val call = routes.get(itemId.toString)
        lazy val req = makeRequest(call)
        lazy val result = route(req).get
        status(result) must_== BAD_REQUEST
      }.pendingUntilFixed("AC-320")
    }
  }

}

