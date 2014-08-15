package org.corespring.api.v1

import org.corespring.it.IntegrationSpecification
import org.corespring.mongo.json.services.MongoService
import org.corespring.v2.player.scopes.orgWithAccessTokenAndItem
import play.api.Play
import play.api.libs.json.Json
import play.api.test.FakeRequest
import se.radley.plugin.salat.SalatPlugin

import scala.concurrent.{ ExecutionContext, Future }

class ItemApiSessionCountTest extends IntegrationSpecification {

  trait withV2Session extends orgWithAccessTokenAndItem {

    val sessionService = new MongoService(db("v2.itemSessions"))
    val sessionId = sessionService.create(Json.obj("itemId" -> itemId.toString))
    def db = Play.current.plugin[SalatPlugin].get.db()

    override def after = {
      println(s"[withV2Session] after")
      sessionService.delete(sessionId.get.toString)
      super.after
    }
  }

  "ItemApi" should {

    "counts v2 item sessions when calling count sessions" in new withV2Session {
      val call = org.corespring.api.v1.routes.ItemApi.countSessions(itemId)
      route(FakeRequest(call.method, s"${call.url}?access_token=$accessToken")).map { result =>
        status(result) === 200
        contentAsJson(result) === Json.obj("sessionCount" -> 1)
      }.getOrElse(failure)
    }
  }
}
