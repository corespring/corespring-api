package org.corespring.it.helpers

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ User, UserOrg }

object UserHelper {

  import faker._

  lazy val service = bootstrap.Main.userService

  def create(orgId: ObjectId, fullName: String = Name.name, permission: Permission = Permission.Write) = {

    println(s"[UserHelper] create user $fullName")
    val user = User(
      fullName = fullName,
      userName = fullName.toLowerCase(),
      email = Internet.email(fullName),
      org = UserOrg(orgId, permission.value))

    service.insertUser(user, orgId, permission)
    user
  }

  def delete(userId: ObjectId) = {
    println(s"[UserHelper] delete user $userId")
    service.removeUser(userId)
  }

}
