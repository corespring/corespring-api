package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User, UserOrg }
import org.specs2.mutable.After

import scalaz.Success

class UserServiceTest extends ServicesSalatIntegrationTest {

  trait UserScoped extends After {

    val org = Organization(id = ObjectId.get, name = "saved user test org")

    val user = User("user_test_name", org = UserOrg(org.id, Permission.Read.value))

    services.orgService.insert(org, None)
    services.userService.insertUser(user, org.id, Permission.Write)

    def after {
      services.userService.removeUser(user.userName)
      services.orgService.delete(org.id)
    }
  }

  "userService" should {

    "getPermissions" should {
      "work if the org and userName is in the db" in new UserScoped() {
        services.userService.getPermissions(user.userName, org.id) must_== Success(Permission(3, "write"))
      }
      "fail if there is the org is not in the db" in new UserScoped() {
        val nonExistentOrgId = ObjectId.get
        services.userService.getPermissions(user.userName, nonExistentOrgId).fold(e => success, _ => failure)
      }
      "fail if the username is not in the db" in new UserScoped() {
        services.userService.getPermissions("non existent name", org.id).fold(e => success, _ => failure)
      }
    }

    "getOrg" should {
      "return the organisation of the user" in new UserScoped() {
        val result = services.userService.getOrg(user, Permission.Read)
        result !== None
        val dbOrg = result.get
        dbOrg.name === org.name
        dbOrg.id === org.id
      }
      "fail is user does not have the expected permissions" in new UserScoped() {
        val result = services.userService.getOrg(user, Permission.Write)
        result === None
      }
    }

    /*
    "getUser" in pending
    "getUserByEmail" in pending
    "getUsers" in pending
    "insertUser" in pending
    "removeUser" in pending
    "setOrganization" in pending
    "touchLastLogin" in pending
    "touchRegistration" in pending
    "updateUser" in pending

    "insert" in pending
    "update" in pending
    "add organization" in pending
    "get organization" in pending
    "get" in pending
    "write json" in pending
    */
  }
}
