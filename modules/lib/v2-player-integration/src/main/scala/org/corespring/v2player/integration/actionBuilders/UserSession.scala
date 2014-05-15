package org.corespring.v2player.integration.actionBuilders

import org.corespring.platform.core.models.User
import org.corespring.platform.core.services.UserService
import org.corespring.v2player.integration.securesocial.SecureSocialService
import play.api.mvc.RequestHeader

trait UserSession {

  def secureSocialService: SecureSocialService
  def userService: UserService

  def userFromSession(request: RequestHeader): Option[User] = for {
    ssUser <- secureSocialService.currentUser(request)
    dbUser <- userService.getUser(ssUser.identityId.userId, ssUser.identityId.providerId)
  } yield dbUser
}
