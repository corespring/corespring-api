package org.corespring.v2.api

import grizzled.slf4j.Logger
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scope.scopes.orgWithAccessTokenAndItem
import play.api.test.FakeRequest

class ItemApiTest extends IntegrationSpecification {

  override protected def logger: Logger = grizzled.slf4j.Logger(this.getClass)

  "GET /:itemId" should {
    "return OK" in new orgWithAccessTokenAndItem {
      val get = org.corespring.v2.api.routes.ItemApi.get(itemId.toString)
      route(FakeRequest(get.method, s"${get.url}=access_token=$accessToken")).map { r =>
        status(r) === OK
      }.getOrElse(ko)
    }
  }
}

class ItemApiTwoTest extends IntegrationSpecification {

  override protected def logger: Logger = grizzled.slf4j.Logger(this.getClass)

  "GET /:itemId" should {
    "return OK" in new orgWithAccessTokenAndItem {
      val get = org.corespring.v2.api.routes.ItemApi.get(itemId.toString)
      route(FakeRequest(get.method, s"${get.url}=access_token=$accessToken")).map { r =>
        status(r) === OK
      }.getOrElse(ko)
    }
  }
}
