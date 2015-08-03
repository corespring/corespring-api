package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User, UserOrg }
import org.specs2.mutable.After

class UserServiceTest extends ServicesSalatIntegrationTest {

  class UserScoped(saveOrg: Boolean) extends After {

    lazy val user = User("user_test_name", org = UserOrg(org.id, Permission.Read.value))

    lazy val org = getOrg

    def getOrg = {
      val o = Organization(id = ObjectId.get, name = "user test org")
      if (saveOrg) {
        services.orgService.insert(o, None)
      }
      o
    }

    def after {
      services.orgService.delete(org.id)
      services.userService.removeUser(user.userName)
    }
  }

  "user" should {

    "insert" in pending
    "update" in pending
    "add organization" in pending
    "get organization" in pending
    "get" in pending
    "write json" in pending

    "get permissions works if the org is in the db" in new UserScoped(true) {
      services.userService.insertUser(user, org.id, Permission.Write)
      services.userService.getPermissions(user.userName, org.id) === Right(Permission(3, "write"))
    }

    "get permissions fails if there is no org in the db" in new UserScoped(false) {
      services.userService.insertUser(user, org.id, Permission.Write)
      services.userService.getPermissions(user.userName, org.id).fold(e => success, _ => failure)
    }
  }

}
