package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.v2.player.scopes._
import org.specs2.specification.BeforeAfter
import play.api.mvc.Call


class V1ItemSessionApiProxyTest extends IntegrationSpecification {
  val Routes = org.corespring.v2.api.routes.V1ItemSessionApiProxy

  "V2 - V1ItemSessionApiProxy" should {

    "reopen" should {

      s"return $OK" in new withItemAndSession {
        status(result) === OK
      }
    }
  }

  class withItemAndSession extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndV1Session {
    override def getCall(sessionId: ObjectId): Call = Routes.reopen(itemId, sessionId)
  }

}
