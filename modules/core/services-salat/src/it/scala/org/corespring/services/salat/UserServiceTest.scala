package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User, UserOrg }
import org.specs2.mutable.After

import scalaz.{ Failure, Success }

class UserServiceTest extends ServicesSalatIntegrationTest {

  trait UserScoped extends After {

    val userService = services.userService

    val org = Organization(id = ObjectId.get, name = "first user test org")
    val userPermission = Permission.Read
    val userOrg = UserOrg(org.id, userPermission.value)
    val user = User(userName = "user_test_name", org = userOrg, provider = "user test provider", email = "user@example.org")
    val nonExistentOrgId = ObjectId.get

    services.orgService.insert(org, None)
    userService.insertUser(user, userOrg.orgId, userPermission)

    def after {
      userService.removeUser(user.userName)
      services.orgService.delete(org.id)
    }

    def assertUser(dbUser: Option[User], user: User) = {
      dbUser !== None
      val dbu = dbUser.get
      dbu.userName === user.userName
      dbu.email === user.email
      dbu.id === user.id
      assertUserOrg(dbu, user.org.orgId, user.org.pval)
    }

    def assertUserOrg(user: User, orgId: ObjectId, pval: Long) = {
      user.org.orgId === orgId
      user.org.pval === pval
    }

    def assertOrgNameAndId(dbOrg: Organization, org: Organization) = {
      dbOrg.name === org.name
      dbOrg.id === org.id
    }
  }

  "userService" should {

    "getPermissions" should {
      "return permissions for the user with org and userName" in new UserScoped() {
        userService.getPermissions(user.userName, org.id) match {
          case Success(p) => p === userPermission
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
        userService.getOrg(user, Permission.Read) match {
          case None => failure("Expected the organization to be returned.")
          case Some(dbOrg) => assertOrgNameAndId(dbOrg, org)
        }
      }

      "fail if user does not have the expected permission" in new UserScoped() {
        userService.getOrg(user, Permission.Write) === None
      }
    }

    "getUser" should {

      "find the user by name" in new UserScoped() {
        val result = userService.getUser(user.userName)
        assertUser(result, user)
      }
      "find the user by name & provider" in new UserScoped() {
        val result = userService.getUser(user.userName, user.provider)
        assertUser(result, user)
      }
      "find the user by id" in new UserScoped() {
        val result = userService.getUser(user.id)
        assertUser(result, user)
      }
      "return none if user name cannot be found" in new UserScoped() {
        userService.getUser("name does not exist in db") === None
      }
      "return none if provider cannot be found" in new UserScoped() {
        userService.getUser(user.userName, "name does not exist in db") === None
      }
      "return none if id cannot be found" in new UserScoped() {
        userService.getUser(ObjectId.get) === None
      }
    }

    "getUserByEmail" should {
      "find the user by email" in new UserScoped() {
        val result = userService.getUserByEmail("user@example.org")
        assertUser(result, user)
      }
      "return none if email cannot be found" in new UserScoped() {
        userService.getUserByEmail("no email") === None
      }
    }

    "getUsers" should {
      "return users for org" in new UserScoped() {
        userService.getUsers(org.id) match {
          case Failure(error) => failure(s"Unexpected failure: $error")
          case Success(users) => {
            assertUser(Some(users(0)), user)
            //TODO Should we return the stream instead of a seq? The seq doesn't seem to work like a seq
            //users.length === 1 //fails with npe in DBApiLayer
          }
        }
      }
      "return empty seq if org does not have users" in new UserScoped() {
        userService.getUsers(nonExistentOrgId) match {
          case Success(users) => users === Seq.empty
          case Failure(error) => failure(s"Unexpected failure: $error")
        }
      }
    }

    "insertUser" should {

      trait InsertUserScope extends UserScoped {
        val newUserPermission = Permission.Read
        val newOrg = new UserOrg(ObjectId.get, newUserPermission.value)
        val newUser = new User(userName = "ralf", org = newOrg)

        override def after = {
          userService.removeUser(newUser.id)
          super.after
        }

        def assertUpdatedUser(updatedUser: User, orgId: ObjectId) = {
          assertUserOrg(updatedUser, orgId, userPermission.value)

          val dbUser = userService.getUser(updatedUser.id)
          assertUser(dbUser, updatedUser)
        }
      }

      "replace the org in user with the given organization" in new InsertUserScope() {
        userService.insertUser(newUser, org.id, userPermission) match {
          case Failure(error) => failure(s"Unexpected error $error")
          case Success(updatedUser) => assertUpdatedUser(updatedUser, org.id)
        }
      }

      "fail if org is not in db" in new InsertUserScope() {
        userService.insertUser(newUser, nonExistentOrgId, userPermission) match {
          case Failure(error) => success
          case Success(updatedUser) => failure("Should have failed bc. org does not exist in db")
        }
      }

      "fail when checkOrg is true and org is not in db" in new InsertUserScope() {
        userService.insertUser(newUser, nonExistentOrgId, userPermission, checkOrgId = true) match {
          case Failure(error) => success
          case Success(updatedUser) => failure("Should have failed bc. org does not exist in db")
        }
      }

      "allow to insert a non-existing orgId when checkOrgId is false" in new InsertUserScope() {
        userService.insertUser(newUser, nonExistentOrgId, userPermission, checkOrgId = false) match {
          case Failure(error) => failure(s"Unexpected error $error")
          case Success(updatedUser) => assertUpdatedUser(updatedUser, nonExistentOrgId)
        }
      }

      "fail when userName is same as an existing user" in new InsertUserScope() {
        val doppelgaenger = user.copy(id = ObjectId.get)
        userService.insertUser(doppelgaenger, org.id, userPermission) match {
          case Failure(error) => success
          case Success(updatedUser) => failure("Should have failed bc. user exists already.")
        }
      }

      "fail when userName is same as an existing user and checkUsername = true" in new InsertUserScope() {
        val doppelgaenger = user.copy(id = ObjectId.get)
        userService.insertUser(doppelgaenger, org.id, userPermission, checkUsername = true) match {
          case Failure(error) => success
          case Success(updatedUser) => failure("Should have failed bc. user exists already.")
        }
      }

      "allow to insert a user with the same name as an existing user when checkUsername = false" in new InsertUserScope() {
        val doppelgaenger = user.copy(id = ObjectId.get)
        userService.insertUser(doppelgaenger, org.id, userPermission, checkUsername = false) match {
          case Failure(error) => failure(s"Unexpected error $error")
          case Success(updatedUser) => {
            val dbUser = userService.getUser(updatedUser.id).get
            dbUser.id !== user.id
            dbUser.userName === user.userName
          }
        }
      }
    }

    "removeUser" should {
      "remove a user by id" in new UserScoped() {
        userService.getUser(user.id)
        userService.removeUser(user.id) match {
          case Failure(e) => failure(s"Unexpected error $e")
          case Success(_) => success
        }
        userService.getUser(user.id) === None
      }

      //TODO What if we have users with the same name
      "remove a user by name" in new UserScoped() {
        userService.getUser(user.id)
        userService.removeUser(user.id) match {
          case Failure(e) => failure(s"Unexpected error $e")
          case Success(_) => success
        }
        userService.getUser(user.id) === None
      }
      "succeed even when user id not in db" in new UserScoped() {
        userService.removeUser(ObjectId.get) match {
          case Failure(e) => failure(s"Unexpected error $e")
          case Success(_) => success
        }
      }
      "fail when user name not in db" in new UserScoped() {
        userService.removeUser("non existent name") match {
          case Failure(e) => success
          case Success(_) => failure("Should have failed bc. name is not in db.")
        }
      }

    }

    "setOrganization" should {
      "update the org and permission" in new UserScoped() {
        val orgId = ObjectId.get //Note, org does not have to exist
        val perms = Permission.None

        userService.setOrganization(user.id, orgId, perms) match {
          case Success(_) => success
          case Failure(e) => failure(s"Unexpected failure $e")
        }

        val dbUser = userService.getUser(user.id).get
        assertUserOrg(dbUser, orgId, perms.value)
      }
    }

    "touchLastLogin" should {
      "work" in new UserScoped() {
        userService.touchLastLogin(user.userName)
        val firstDbUser = userService.getUser(user.id).get
        userService.touchLastLogin(user.userName)
        val secondDbUser = userService.getUser(user.id).get
        firstDbUser.lastLoginDate !== secondDbUser.lastLoginDate
      }
    }

    "touchRegistration" should {
      "work" in new UserScoped() {
        userService.touchRegistration(user.userName)
        val firstDbUser = userService.getUser(user.id).get
        userService.touchRegistration(user.userName)
        val secondDbUser = userService.getUser(user.id).get
        firstDbUser.registrationDate !== secondDbUser.registrationDate
      }
    }

    "updateUser" should {
      "return the updated user" in new UserScoped() {
        userService.updateUser(user.copy(userName="new")) match {
          case Success(u) => assertUser(userService.getUser(user.id), u)
          case Failure(e) => failure(s"Unexpected error: $e")
        }
      }
      "update userName" in new UserScoped() {
        userService.updateUser(user.copy(userName="new"))
        userService.getUser(user.id).get.userName === "new"
      }
      "update fullName" in new UserScoped() {
        userService.updateUser(user.copy(fullName="new"))
        userService.getUser(user.id).get.fullName === "new"
      }
      "update email" in new UserScoped() {
        userService.updateUser(user.copy(email="new"))
        userService.getUser(user.id).get.email === "new"
      }
      "update password" in new UserScoped() {
        userService.updateUser(user.copy(password="new"))
        userService.getUser(user.id).get.password === "new"
      }
      "fail when userId in user is not in db" in new UserScoped() {
        userService.updateUser(user.copy(id = ObjectId.get)) match {
          case Success(u) => failure("Update should have failed bc. id does not exist in db.")
          case Failure(e) => success
        }
      }
    }
  }
}
