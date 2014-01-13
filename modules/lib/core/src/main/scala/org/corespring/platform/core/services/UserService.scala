package org.corespring.platform.core.services

import org.corespring.platform.core.models.User

trait UserService {
  def getUser(username:String, provider: String) : Option[User]
}

object UserServiceWired extends UserService{
  def getUser(username: String, provider: String): Option[User] = User.getUser(username, provider)
}
