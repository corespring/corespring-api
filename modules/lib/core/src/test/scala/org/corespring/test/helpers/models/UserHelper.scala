package org.corespring.test.helpers.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.{UserOrg, User}
import org.corespring.platform.core.models.auth.Permission

object UserHelper {

  import faker._

  def create(organizationId: ObjectId, fullName: String = Name.name, permission: Permission = Permission.Write) = {
    val user = User(
      fullName = fullName,
      userName = fullName.toLowerCase(),
      email = Internet.email(fullName),
      org = UserOrg(organizationId, permission.value)
    )
    User.insert(user)
    user
  }

  def delete(userId: ObjectId) = User.removeUser(userId)

}
