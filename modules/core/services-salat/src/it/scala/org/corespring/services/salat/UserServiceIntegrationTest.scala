package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User, UserOrg }
import org.specs2.mutable.After

import scalaz.{ Failure, Success }

class UserServiceIntegrationTest extends ServicesSalatIntegrationTest {

  trait scope extends After {

    val userService = services.userService

    val org = services.orgService.insert(Organization(id = ObjectId.get, name = "first user test org"), None).toOption.get
    val userPermission = Permission.Read
    val userOrg = UserOrg(org.id, userPermission.value)
    val user = User(userName = "user_test_name", org = userOrg, provider = "user test provider", email = "user@example.org")
    val nonExistentOrgId = ObjectId.get

    userService.insertUser(user)

    def after = removeAllData()
  }

  "userService" should {

    "getPermissions" should {
      "return permissions for the user with org and userName" in new scope {
        userService.getPermissions(user.userName, org.id) must_== Success(Some(userPermission))
      }

      "fail if there is the org is not in the db" in new scope {
        userService.getPermissions(user.userName, nonExistentOrgId).fold(e => success, _ => failure)
      }

      "fail if the username is not in the db" in new scope {
        userService.getPermissions("non existent name", org.id).fold(e => success, _ => failure)
      }
    }

    "getOrg" should {
      "return the organisation of the user" in new scope {
        userService.getOrg(user, Permission.Read) must_== Some(org)
      }

      "fail if user does not have the expected permission" in new scope {
        userService.getOrg(user, Permission.Write) must_== None
      }
    }

    "getUser" should {

      "find the user by name" in new scope {
        userService.getUser(user.userName) must_== Some(user)
      }

      "find the user by name & provider" in new scope {
        userService.getUser(user.userName, user.provider) must_== Some(user)
      }

      "find the user by id" in new scope {
        userService.getUser(user.id) must_== Some(user)
      }

      "return none if user name cannot be found" in new scope {
        userService.getUser("name does not exist in db") must_== None
      }
      "return none if provider cannot be found" in new scope {
        userService.getUser(user.userName, "name does not exist in db") must_== None
      }
      "return none if id cannot be found" in new scope {
        userService.getUser(ObjectId.get) must_== None
      }
    }

    "getUserByEmail" should {
      "find the user by email" in new scope {
        userService.getUserByEmail("user@example.org") must_== Some(user)
      }

      "return none if email cannot be found" in new scope {
        userService.getUserByEmail("no email") must_== None
      }
    }

    "getUsers" should {
      "return users for org" in new scope {
        userService.getUsers(org.id) must_== Stream(user)
      }

      "return empty seq if org does not have users" in new scope {
        userService.getUsers(nonExistentOrgId) must_== Stream.empty
      }
    }

    "insertUser" should {

      trait insertUser extends scope {
        val newUserPermission = Permission.Read
        val newOrg = new UserOrg(ObjectId.get, newUserPermission.value)
        val newUser = new User(userName = "ralf", org = newOrg)
        val doppelgaenger = user.copy(id = ObjectId.get)

        override def after = {
          userService.removeUser(newUser.id)
          super.after
        }

      }

      "fail if org is not in db" in new insertUser {
        userService.insertUser(newUser.copy(org = UserOrg(nonExistentOrgId, Permission.Write.value))) must_== Failure(_: PlatformServiceError)
      }

      "fail when userName is same as an existing user" in new insertUser {
        userService.insertUser(doppelgaenger) must_== Failure(_: PlatformServiceError)
      }

      "insert the user" in new insertUser {
        userService.getUser(user.userName) must_== Some(user)
      }
    }

    "removeUser" should {
      "remove a user by id" in new scope {
        userService.removeUser(user.id) must_== Success()
        userService.getUser(user.id) must_== None
      }

      //TODO What if we have users with the same name
      "remove a user by name" in new scope {
        userService.removeUser(user.id) must_== Success()
        userService.getUser(user.id) must_== None
      }

      //TODO: Shouldn't it fail?
      "succeed even when user id not in db" in new scope {
        userService.removeUser(ObjectId.get) must_== Success()
      }

      "fail when user name not in db" in new scope {
        userService.removeUser("non existent name") must_== Failure(_: PlatformServiceError)
      }

    }

    "setOrganization" should {
      "update the org and permission" in new scope {
        val orgId = ObjectId.get //Note, org does not have to exist
        val perms = Permission.Read
        userService.setOrganization(user.id, orgId, perms) must_== Success()
        userService.getUser(user.id) must_== Some(user.copy(org = UserOrg(orgId, perms.value)))
      }
    }

    "touchLastLogin" should {
      "work" in new scope {
        userService.touchLastLogin(user.userName)
        val first = userService.getUser(user.id).map(_.lastLoginDate)
        Thread.sleep(10)
        userService.touchLastLogin(user.userName)
        val second = userService.getUser(user.id).map(_.lastLoginDate)
        first !== second
      }
    }

    "touchRegistration" should {
      "work" in new scope {
        userService.touchRegistration(user.userName)
        val first = userService.getUser(user.id).map(_.registrationDate)
        Thread.sleep(10)
        userService.touchRegistration(user.userName)
        val second = userService.getUser(user.id).map(_.registrationDate)
        first.isDefined must_== true
        first !== second
      }
    }

    "updateUser" should {
      "return the updated user" in new scope {
        val updatedUser = userService.updateUser(user.copy(userName = "new")).toOption
        userService.getUser(user.id) must_== updatedUser
      }

      "not update if userName is already taken by someone else" in new scope {
        userService.insertUser(User("other-user", org = userOrg))
        userService.updateUser(user.copy(userName = "other-user")) must_== Failure(_: PlatformServiceError)
      }

      "update userName" in new scope {
        userService.updateUser(user.copy(userName = "new"))
        userService.getUser(user.id).map(_.userName) must_== Some("new")
      }

      "update fullName" in new scope {
        userService.updateUser(user.copy(fullName = "new"))
        userService.getUser(user.id).map(_.fullName) must_== Some("new")
      }

      "update email" in new scope {
        userService.updateUser(user.copy(email = "new"))
        userService.getUser(user.id).map(_.email) must_== Some("new")
      }

      "update password" in new scope {
        userService.updateUser(user.copy(password = "new"))
        userService.getUser(user.id).map(_.password) must_== Some("new")
      }

      "fail when userId in user is not in db" in new scope {
        userService.updateUser(user.copy(id = ObjectId.get)) must_== Failure(_: PlatformServiceError)
      }
    }
  }
}
