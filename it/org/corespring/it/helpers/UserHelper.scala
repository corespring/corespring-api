package org.corespring.it.helpers

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ User, UserOrg }
import play.api.Logger

object UserHelper {

  import faker._

  val logger = Logger(UserHelper.getClass)

  lazy val service = bootstrap.Main.userService

  def create(orgId: ObjectId, fullName: String = Name.name, permission: Permission = Permission.Write) = {

    logger.info(s"create user $fullName")
    val user = User(
      fullName = fullName,
      userName = fullName.toLowerCase(),
      email = Internet.email(fullName),
      org = UserOrg(orgId, permission.value))

    service.insertUser(user)
    user
  }

  def delete(userId: ObjectId) = {
    logger.info(s"delete user $userId")
    service.removeUser(userId)
  }

}
