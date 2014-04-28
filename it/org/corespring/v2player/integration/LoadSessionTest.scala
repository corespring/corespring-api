package org.corespring.v2player.integration

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.test.TestModelHelpers
import org.corespring.v2player.integration.actionBuilders.access.{ V2PlayerCookieKeys, PlayerOptions }
import org.corespring.v2player.integration.scopes.sessionData
import org.specs2.specification.Example
import play.api.test.FakeRequest

class LoadSessionTest extends IntegrationSpecification with TestModelHelpers {

  override lazy val logger = org.slf4j.LoggerFactory.getLogger("it.loadSesson")

  "when I load a session" should {

    "fail if there is no session cookie" in withSessionParams()
    "succeed if there is an anonymous user session cookie" in withSessionParams(OK, (orgId) => {
      import play.api.libs.json.Json._
      Seq(
        V2PlayerCookieKeys.orgId -> orgId.toString,
        V2PlayerCookieKeys.renderOptions -> stringify(toJson(PlayerOptions.ANYTHING)))
    })
  }

  def withSessionParams(expectedStatus: Int = UNAUTHORIZED, addSessionCookies: (ObjectId => Seq[(String, String)]) = oid => Seq.empty): Example = Example(
    s"withSessionParams: $expectedStatus, $addSessionCookies",
    new sessionData {

      import org.corespring.container.client.controllers.resources.routes.Session

      val call = Session.loadEverything(sessionId.toString)
      val request = FakeRequest(call.method, call.url).withSession(addSessionCookies(orgId): _*)
      route(request) match {
        case Some(result) => status(result) === expectedStatus
        case _ => failure(s"${call.url} returned nothing")
      }
    })
}
