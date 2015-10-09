package org.corespring.services.salat

import com.mongodb.{WriteConcern, DBObject}
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatDAOUpdateError, SalatMongoCursor, SalatRemoveError }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User, UserOrg }
import org.corespring.services.errors.PlatformServiceError
import org.corespring.{ services => interface }
import org.joda.time.DateTime

import scalaz.{ Failure, Success, Validation }

class UserService(
  val dao: SalatDAO[User, ObjectId],
  val context: Context,
  orgService: interface.OrganizationService) extends interface.UserService with HasDao[User, ObjectId] {

  def logger: Logger = Logger(classOf[UserService])

  private implicit val ctx = context

  /**
   * insert a user into the database as a member of the given organization,
   * along with their private organization and collection
   * @param user
   * @param orgId - the organization that the given user belongs to
   * @return the user that was inserted
   */
  override def insertUser(user: User, orgId: ObjectId, p: Permission, checkOrgId: Boolean, checkUsername: Boolean): Validation[PlatformServiceError, User] = {
    if (!checkOrgId || orgService.findOneById(orgId).isDefined) {
      if (!checkUsername || getUser(user.userName).isEmpty) {
        val update = user.copy(org = UserOrg(orgId, p.value))
        dao.insert(update, com.mongodb.WriteConcern.ACKNOWLEDGED) match {
          case Some(id) => {
            Success(update)
          }
          case None => Failure(PlatformServiceError("error inserting user"))
        }
      } else Failure(PlatformServiceError("user already exists"))
    } else Failure(PlatformServiceError("no organization found with given id"))
  }

  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */

  override def getUser(username: String): Option[User] = getUser(MongoDBObject("userName" -> username))

  override def getUser(userId: ObjectId): Option[User] = getUser(MongoDBObject("_id" -> userId))

  override def getUser(username: String, provider: String): Option[User] = getUser(MongoDBObject("userName" -> username, "provider" -> provider))

  override def getUserByEmail(email: String): Option[User] = getUser(MongoDBObject("email" -> email))

  private def getUser(query:DBObject) : Option[User] = {
    logger.debug(s"[getUser]: query $query")
    val result = dao.findOne(query)
    logger.debug(s"[getUser]: result $result")
    result
  }

  override def getPermissions(username: String, orgId: ObjectId): Validation[PlatformServiceError, Permission] = {
    dao.findOne(MongoDBObject("userName" -> username, "org.orgId" -> orgId)) match {
      case Some(u) => getPermissions(u, orgId)
      case None => Failure(PlatformServiceError(s"could not find user $username with access to given organization: $orgId"))
    }
  }

  private def getPermissions(user: User, orgId: ObjectId): Validation[PlatformServiceError, Permission] = {
    Permission.fromLong(user.org.pval) match {
      case Some(p) => Success(p)
      case None => Failure(PlatformServiceError(s"Unknown permission retrieved: ${user.org.pval}"))
    }
  }

  override def getUsers(orgId: ObjectId): Validation[PlatformServiceError, Seq[User]] = {
    val c: SalatMongoCursor[User] = dao.find(MongoDBObject("org.orgId" -> orgId))
    val returnValue = Success(c.toSeq)
    c.close()
    returnValue
  }

  override def touchLastLogin(userName: String) = touch(userName, "lastLoginDate")
  override def touchRegistration(userName: String) = touch(userName, "registrationDate")

  private def touch(userName: String, field: String): Unit =
    getUser(userName) match {
      case Some(user) => {
        dao.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
          MongoDBObject(
            field -> new DateTime())),
          false, false, dao.collection.writeConcern)
        Success(user)
      }
      case None => Failure(PlatformServiceError("no user found to update " + field))
    }



  override def updateUser(user: User): Validation[PlatformServiceError, User] = {
    try {
      dao.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
        MongoDBObject(
          "userName" -> user.userName,
          "fullName" -> user.fullName,
          "email" -> user.email,
          "password" -> user.password)),
        false, false, dao.collection.writeConcern)
      getUser(user.id) match {
        case Some(u) => Success(u)
        case None => Failure(PlatformServiceError(s"Failed to update user: user not found for id: ${user.id}"))
      }
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError("failed to update user", e))
    }
  }

  override def getOrg(user: User, p: Permission): Option[Organization] = {
    val org: Option[ObjectId] = if ((user.org.pval & p.value) == p.value) Some(user.org.orgId) else None
    org.flatMap(orgService.findOneById)
  }

  override def setOrganization(userId: ObjectId, orgId: ObjectId, p: Permission): Validation[PlatformServiceError, Unit] = {
    import com.novus.salat.grater

    val userOrg = UserOrg(orgId, p.value)
    try {
      dao.update(MongoDBObject("_id" -> userId),
        MongoDBObject("$set" -> MongoDBObject("org" -> grater[UserOrg].asDBObject(userOrg))),
        false, false, dao.collection.writeConcern)
      Success(())
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError(s"Error updating user $userId with org $orgId and permission ${p.name}"))
    }
  }

  override def removeUser(username: String): Validation[PlatformServiceError, Unit] = {
    getUser(username) match {
      case Some(user) => removeUser(user.id)
      case None => Failure(PlatformServiceError("user could not be removed because it doesn't exist"))
    }
  }

  override def removeUser(userId: ObjectId): Validation[PlatformServiceError, Unit] = {
    try {
      dao.removeById(userId)
      Success(())
    } catch {
      case e: SalatRemoveError => Failure(PlatformServiceError("error occurred while removing user", e))
    }
  }

}
