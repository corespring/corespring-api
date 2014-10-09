package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.v2.player.scopes.{ TokenRequestBuilder, orgWithAccessToken }
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.{ FakeHeaders, FakeRequest }

class PlayerTokenApi extends IntegrationSpecification {

  "PlayerTokenApi" should {
    "create a player token" in new token_createPlayerToken {
      status(result) === OK
    }
  }

  trait token_createPlayerToken extends orgWithAccessToken with TokenRequestBuilder {
    lazy val call = org.corespring.v2.api.routes.PlayerTokenApi.createPlayerToken()

    lazy val result = route(
      FakeRequest(call.method, call.url, FakeHeaders(), AnyContentAsJson(Json.obj()))).getOrElse(throw new RuntimeException("Couldn't get a result"))
  }

}
