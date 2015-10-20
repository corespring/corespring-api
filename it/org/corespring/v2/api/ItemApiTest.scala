package org.corespring.v2.api

import grizzled.slf4j.Logger
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.orgWithAccessTokenAndItem
import play.api.test.FakeRequest

class ItemApiTest extends IntegrationSpecification {

  "GET /:itemId" should {

    s"return $UNAUTHORIZED for request with no access token" in new orgWithAccessTokenAndItem {
      val get = org.corespring.v2.api.routes.ItemApi.get(itemId.toString)
      route(FakeRequest(get.method, get.url)).map { r =>
        status(r) === UNAUTHORIZED
      }.getOrElse(ko)
    }

    "return OK for request with access token" in new orgWithAccessTokenAndItem {
      val get = org.corespring.v2.api.routes.ItemApi.get(itemId.toString)
      route(FakeRequest(get.method, s"${get.url}?access_token=$accessToken")).map { r =>
        println(contentAsString(r))
        status(r) === OK
      }.getOrElse(ko)
    }
  }
}

