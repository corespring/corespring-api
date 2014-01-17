package org.corespring.v2player.integration

import org.bson.types.ObjectId
import org.corespring.it.ITSpec
import org.corespring.player.accessControl.cookies.PlayerCookieKeys
import org.corespring.test.TestModelHelpers
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.scopes.sessionData
import org.specs2.specification.Example
import play.api.test.FakeRequest

class LoadSessionTest extends ITSpec with TestModelHelpers{

  "when I load a session" should {

    "it fails if there is no session cookie" in withSessionParams()
    "it succeeds if there is an anonymous user session cookie" in withSessionParams(OK, (orgId) => {
      import play.api.libs.json.Json._
      Seq(
      PlayerCookieKeys.ORG_ID -> orgId.toString,
      PlayerCookieKeys.RENDER_OPTIONS -> stringify(toJson(PlayerOptions.ANYTHING))
    )})
  }

  def withSessionParams(expectedStatus:Int = UNAUTHORIZED, addSessionCookies: (ObjectId => Seq[(String,String)]) = oid => Seq.empty): Example = Example(
    s"withSessionParams: $expectedStatus, $addSessionCookies",
    new sessionData {

      import org.corespring.container.client.controllers.resources.routes.Session

      val call = Session.loadEverything(sessionId.toString)
      val request = FakeRequest(call.method, call.url).withSession(addSessionCookies(orgId): _*)
      route(request) match {
        case Some(result) => status(result) === expectedStatus
        case _ => failure(s"${call.url} returned nothing")
      }
    }
  )
}
