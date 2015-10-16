package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User }
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

trait UserService {

  /**
   * insert a user into the database as a member of the given organization, along with their private organization and collection
   * @param user
   * @param orgId - the organization that the given user belongs to
   * @return the user that was inserted
   */
  @deprecated("refactor to insertUser(User,Permission) instead", "core-refactor")
  def insertUser(user: User, orgId: ObjectId, p: Permission, checkOrgId: Boolean = true, checkUsername: Boolean = true): Validation[PlatformServiceError, User]

  def removeUser(userName: String): Validation[PlatformServiceError, Unit]

  def removeUser(userId: ObjectId): Validation[PlatformServiceError, Unit]

  def touchLastLogin(userName: String)
  def touchRegistration(userName: String)

  def updateUser(user: User): Validation[PlatformServiceError, User]

  def setOrganization(userId: ObjectId, orgId: ObjectId, p: Permission): Validation[PlatformServiceError, Unit]

  def getOrg(user: User, p: Permission): Option[Organization]

  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */
  def getUser(username: String): Option[User]

  def getUser(userId: ObjectId): Option[User]

  def getUser(username: String, provider: String): Option[User]

  def getUserByEmail(email: String): Option[User]

  def getUsers(orgId: ObjectId): Stream[User]

  def getPermissions(username: String, orgId: ObjectId): Validation[PlatformServiceError, Option[Permission]]
}

