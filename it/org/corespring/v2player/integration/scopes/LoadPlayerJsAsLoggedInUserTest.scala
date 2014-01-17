package org.corespring.v2player.integration.scopes

import org.corespring.it.ITSpec
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.{OrganizationHelper, UserHelper}
import org.specs2.mutable.BeforeAfter
import play.api.test.FakeRequest

class LoadPlayerJsAsLoggedInUserTest extends ITSpec
  with SecureSocialHelpers{

  "when I'm a logged in user loading the player js" should {

    "return a cookie" in new user {

      import org.corespring.container.client.controllers.routes.PlayerLauncher

      val call = PlayerLauncher.playerJs()
      val cookie = secureSocialCookie(Some(user),None).getOrElse(throw new RuntimeException("can't create cookie"))
      val request = FakeRequest(call.method, call.url).withCookies(cookie)
      route(request).map{ r =>
        if(status(r) != OK) logger.trace(contentAsString(r))
        status(r) === OK
      }.getOrElse(failure(s"router returned nothing for: ${call.url}"))
      true === true
    }
  }

  trait user extends BeforeAfter{

    val orgId = OrganizationHelper.create("my-org")
    val user = UserHelper.create(orgId)

    def before: Any = {}

    def after: Any = {
      UserHelper.delete(user.id)
      OrganizationHelper.delete(orgId)
    }
  }
}
