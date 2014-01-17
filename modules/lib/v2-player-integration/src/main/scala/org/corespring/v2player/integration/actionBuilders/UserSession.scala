package org.corespring.v2player.integration.actionBuilders

import play.api.mvc.{AnyContent, Request}
import org.corespring.platform.core.models.User
import org.corespring.v2player.integration.securesocial.SecureSocialService
import org.corespring.platform.core.services.UserService

trait UserSession {

  def secureSocialService : SecureSocialService
  def userService : UserService

  def userFromSession(request: Request[AnyContent]): Option[User] = for {
    ssUser <- secureSocialService.currentUser(request)
    dbUser <- userService.getUser(ssUser.identityId.userId, ssUser.identityId.providerId)
  } yield dbUser
}
