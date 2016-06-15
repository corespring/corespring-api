package org.corespring.v2.player

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ V2SessionHelper, IntegrationHelpers }
import org.corespring.it.scopes._
import org.specs2.specification.Scope
import play.api.libs.json.Json

class LoadPlayerBySessionIdIntegrationTest
  extends IntegrationSpecification
  with IntegrationHelpers {

  trait scope
    extends Scope
    with orgWithAccessTokenAndItem
    with clientIdAndPlayerToken
    with WithV2SessionHelper
    with IdAndPlayerTokenRequestBuilder {

    lazy val sessionId = v2SessionHelper.create(itemId)
    lazy val call = org.corespring.container.client.controllers.apps.routes.Player.load(sessionId.toString)
    lazy val req = makeRequest(call)
    lazy val result = route(req).get

    override def skipDecryption: Boolean = false

    override lazy val playerToken: String = {
      val result = main.playerTokenService.createToken(apiClient, Json.obj("expires" -> 0)).toOption.get
      result.token
    }
  }

  "load by session id" should {

    "not create any duplicate sessions accidentally" in new scope {
      v2SessionHelper.count must_== 0
      status(result) must_== 200
      v2SessionHelper.count must_== 1
    }
  }
}
