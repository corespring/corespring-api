package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User, UserOrg }
import org.corespring.services.errors.PlatformServiceError
import org.specs2.mutable.After

import scalaz.{ Failure, Success }

class UserServiceTest extends ServicesSalatIntegrationTest {

  trait UserScoped extends After {

    val userService = services.userService

    val org = services.orgService.insert(Organization(id = ObjectId.get, name = "first user test org"), None).toOption.get
    val userPermission = Permission.Read
    val userOrg = UserOrg(org.id, userPermission.value)
    val user = User(userName = "user_test_name", org = userOrg, provider = "user test provider", email = "user@example.org")
    val nonExistentOrgId = ObjectId.get

    userService.insertUser(user, userOrg.orgId, userPermission)

    def after {
      userService.removeUser(user.userName)
      services.orgService.delete(org.id)
    }

  }

  "userService" should {

    "getPermissions" should {
      "return permissions for the user with org and userName" in new UserScoped() {
        userService.getPermissions(user.userName, org.id) match {
          case Success(p) => p must_== userPermission
          case Failure(e) => failure(s"Unexpected error $e")
        }
      }
      "fail if there is the org is not in the db" in new UserScoped() {
        userService.getPermissions(user.userName, nonExistentOrgId).fold(e => success, _ => failure)
      }
      "fail if the username is not in the db" in new UserScoped() {
        userService.getPermissions("non existent name", org.id).fold(e => success, _ => failure)
      }
    }

    "getOrg" should {
      "return the organisation of the user" in new UserScoped() {
        userService.getOrg(user, Permission.Read) must_== Some(org)
      }

      "fail if user does not have the expected permission" in new UserScoped() {
        userService.getOrg(user, Permission.Write) must_== None
      }
    }

    "getUser" should {

      "find the user by name" in new UserScoped() {
        userService.getUser(user.userName) must_== Some(user)
      }

      "find the user by name & provider" in new UserScoped() {
        userService.getUser(user.userName, user.provider) must_== Some(user)
      }

      "find the user by id" in new UserScoped() {
        userService.getUser(user.id) must_== Some(user)
      }

      "return none if user name cannot be found" in new UserScoped() {
        userService.getUser("name does not exist in db") must_== None
      }
      "return none if provider cannot be found" in new UserScoped() {
        userService.getUser(user.userName, "name does not exist in db") must_== None
      }
      "return none if id cannot be found" in new UserScoped() {
        userService.getUser(ObjectId.get) must_== None
      }
    }

    "getUserByEmail" should {
      "find the user by email" in new UserScoped() {
        userService.getUserByEmail("user@example.org") must_== Some(user)
      }

      "return none if email cannot be found" in new UserScoped() {
        userService.getUserByEmail("no email") must_== None
      }
    }

    "getUsers" should {
      "return users for org" in new UserScoped() {
        userService.getUsers(org.id) must_== Stream(user)
      }

      "return empty seq if org does not have users" in new UserScoped() {
        userService.getUsers(nonExistentOrgId) must_== Stream.empty
      }
    }

    "insertUser" should {

      trait InsertUserScope extends UserScoped {
        val newUserPermission = Permission.Read
        val newOrg = new UserOrg(ObjectId.get, newUserPermission.value)
        val newUser = new User(userName = "ralf", org = newOrg)
        val doppelgaenger = user.copy(id = ObjectId.get)

        override def after = {
          userService.removeUser(newUser.id)
          super.after
        }

      }

      "replace the org in user with the given organization" in new InsertUserScope() {
        userService.insertUser(newUser, org.id, userPermission) must_== Success(newUser.copy(org = UserOrg(org.id, userPermission.value)))
      }

      "fail if org is not in db" in new InsertUserScope() {
        userService.insertUser(newUser, nonExistentOrgId, userPermission) must_== Failure(_: PlatformServiceError)
      }

      "fail when checkOrg is true and org is not in db" in new InsertUserScope() {
        userService.insertUser(newUser, nonExistentOrgId, userPermission, checkOrgId = true) must_== Failure(_: PlatformServiceError)
      }

      "allow to insert a non-existing orgId when checkOrgId is false" in new InsertUserScope() {
        userService.insertUser(newUser, nonExistentOrgId, userPermission, checkOrgId = false) must_==
          Success(newUser.copy(org = UserOrg(nonExistentOrgId, userPermission.value)))
      }

      "fail when userName is same as an existing user" in new InsertUserScope() {
        userService.insertUser(doppelgaenger, org.id, userPermission) must_== Failure(_: PlatformServiceError)
      }

      "fail when userName is same as an existing user and checkUsername = true" in new InsertUserScope() {
        userService.insertUser(doppelgaenger, org.id, userPermission, checkUsername = true) must_== Failure(_: PlatformServiceError)
      }

      "allow to insert a user with the same name as an existing user when checkUsername = false" in new InsertUserScope() {
        userService.insertUser(doppelgaenger, org.id, userPermission, checkUsername = false) must_==
          Success(doppelgaenger)
      }
    }

    "removeUser" should {
      "remove a user by id" in new UserScoped() {
        userService.removeUser(user.id) must_== Success()
        userService.getUser(user.id) must_== None
      }

      //TODO What if we have users with the same name
      "remove a user by name" in new UserScoped() {
        userService.removeUser(user.id) must_== Success()
        userService.getUser(user.id) must_== None
      }

      //TODO: Shouldn't it fail?
      "succeed even when user id not in db" in new UserScoped() {
        userService.removeUser(ObjectId.get) must_== Success()
      }

      "fail when user name not in db" in new UserScoped() {
        userService.removeUser("non existent name") must_== Failure(_: PlatformServiceError)
      }

    }

    "setOrganization" should {
      "update the org and permission" in new UserScoped() {
        val orgId = ObjectId.get //Note, org does not have to exist
        val perms = Permission.None
        userService.setOrganization(user.id, orgId, perms) must_== Success()
        userService.getUser(user.id) must_== Some(user.copy(org = UserOrg(orgId, perms.value)))
      }
    }

    "touchLastLogin" should {
      "work" in new UserScoped() {
        userService.touchLastLogin(user.userName)
        val first = userService.getUser(user.id).map(_.lastLoginDate)
        Thread.sleep(10)
        userService.touchLastLogin(user.userName)
        val second = userService.getUser(user.id).map(_.lastLoginDate)
        first !== second
      }
    }

    "touchRegistration" should {
      "work" in new UserScoped() {
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
      "return the updated user" in new UserScoped() {
        val updatedUser = userService.updateUser(user.copy(userName = "new")).toOption
        userService.getUser(user.id) must_== updatedUser
      }

      "update userName" in new UserScoped() {
        userService.updateUser(user.copy(userName = "new"))
        userService.getUser(user.id).map(_.userName) must_== Some("new")
      }

      "update fullName" in new UserScoped() {
        userService.updateUser(user.copy(fullName = "new"))
        userService.getUser(user.id).map(_.fullName) must_== Some("new")
      }

      "update email" in new UserScoped() {
        userService.updateUser(user.copy(email = "new"))
        userService.getUser(user.id).map(_.email) must_== Some("new")
      }

      "update password" in new UserScoped() {
        userService.updateUser(user.copy(password = "new"))
        userService.getUser(user.id).map(_.password) must_== Some("new")
      }

      "fail when userId in user is not in db" in new UserScoped() {
        userService.updateUser(user.copy(id = ObjectId.get)) must_== Failure(_: PlatformServiceError)
      }
    }
  }
}
