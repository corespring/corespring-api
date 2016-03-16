package org.corespring.v2.player

import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.{ IdAndPlayerTokenRequestBuilder, WithV2SessionHelper, clientIdAndPlayerToken, orgWithAccessTokenAndItem }
import org.specs2.specification.Scope
import play.api.libs.json.Json

class CreateSessionForItemIntegrationTest
  extends IntegrationSpecification {

  trait scope
    extends Scope
    with orgWithAccessTokenAndItem
    with clientIdAndPlayerToken
    with WithV2SessionHelper
    with IdAndPlayerTokenRequestBuilder {

    lazy val call = org.corespring.container.client.controllers.apps.routes.Player.createSessionForItem(itemId.toString)
    lazy val req = makeRequest(call)
    lazy val result = route(req).get

    override def skipDecryption: Boolean = false

    override lazy val playerToken: String = {
      val result = main.playerTokenService.createToken(apiClient, Json.obj("expires" -> 0)).toOption.get
      result.token
    }
  }

  "createSessionForItem" should {
    "only creates 1 session" in new scope {
      v2SessionHelper.count(itemId) must_== 0
      status(result) must_== CREATED
      v2SessionHelper.count(itemId) must_== 1
    }
  }
}
