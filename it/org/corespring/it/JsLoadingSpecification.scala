package org.corespring.it

import developer.controllers.SecureSocialHelpers
import org.corespring.v2.player.scopes.user
import play.api.mvc.{ SimpleResult, Call }
import play.api.test.FakeRequest
import scala.concurrent.Future

class JsLoadingSpecification extends IntegrationSpecification
  with SecureSocialHelpers {

  class callAsUser(call: Call) extends user {
    lazy val result: Future[SimpleResult] = {
      val cookie = secureSocialCookie(Some(user), None).getOrElse(throw new RuntimeException("can't create cookie"))
      val request = FakeRequest(call.method, call.url).withCookies(cookie)
      route(request)
    }.getOrElse(throw new RuntimeException("Can't load result"))
  }

}
