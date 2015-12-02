package org.corespring.web.user

import org.corespring.models.User
import org.corespring.services.UserService
import play.api.mvc.RequestHeader

trait UserFromRequest {

  def secureSocial: org.corespring.web.user.SecureSocial
  def userService: UserService

  def userFromSession(request: RequestHeader): Option[User] = for {
    ssUser <- secureSocial.currentUser(request)
    dbUser <- userService.getUser(ssUser.identityId.userId, ssUser.identityId.providerId)
  } yield dbUser
}

