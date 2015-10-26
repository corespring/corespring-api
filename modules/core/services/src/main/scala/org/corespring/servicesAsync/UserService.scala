package org.corespring.servicesAsync

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User }

import scalaz.Validation
import scala.concurrent.Future

trait UserService {

  /**
   * insert a user into the database as a member of the given organization, along with their private organization and collection
   * @param user
   * @return the user that was inserted
   */
  def insertUser(user: User): Future[Validation[PlatformServiceError, User]]

  def removeUser(userName: String): Future[Validation[PlatformServiceError, Unit]]

  def removeUser(userId: ObjectId): Future[Validation[PlatformServiceError, Unit]]

  def touchLastLogin(userName: String): Unit

  def touchRegistration(userName: String): Unit

  def updateUser(user: User): Future[Validation[PlatformServiceError, User]]

  def setOrganization(userId: ObjectId, orgId: ObjectId, p: Permission): Future[Validation[PlatformServiceError, Unit]]

  def getOrg(user: User, p: Permission): Future[Option[Organization]]

  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */
  def getUser(username: String): Future[Option[User]]

  def getUser(userId: ObjectId): Future[Option[User]]

  def getUser(username: String, provider: String): Future[Option[User]]

  def getUserByEmail(email: String): Future[Option[User]]

  def getUsers(orgId: ObjectId): Future[Stream[User]]

  def getPermissions(username: String, orgId: ObjectId): Future[Validation[PlatformServiceError, Option[Permission]]]
}

