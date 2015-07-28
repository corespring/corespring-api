package org.corespring.services.salat

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatDAOUpdateError, SalatMongoCursor, SalatRemoveError }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.services.errors.PlatformServiceError
import org.corespring.{ services => interface }
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User, UserOrg }
import org.joda.time.DateTime

class UserService(
  val dao: SalatDAO[User, ObjectId],
  val context: Context,
  orgService: interface.OrganizationService) extends interface.UserService with HasDao[User, ObjectId] {

  def logger: Logger = Logger(classOf[UserService])

  private implicit val ctx = context

  /**
   * insert a user into the database as a member of the given organization, along with their private organization and collection
   * @param user
   * @param orgId - the organization that the given user belongs to
   * @return the user that was inserted
   */
  override def insertUser(user: User, orgId: ObjectId, p: Permission, checkOrgId: Boolean, checkUsername: Boolean): Either[PlatformServiceError, User] = {
    if (!checkOrgId || orgService.findOneById(orgId).isDefined) {
      if (!checkUsername || getUser(user.userName).isEmpty) {
        val update = user.copy(org = UserOrg(orgId, p.value))
        dao.insert(update) match {
          case Some(id) => {
            Right(user)
          }
          case None => Left(PlatformServiceError("error inserting user"))
        }
      } else Left(PlatformServiceError("user already exists"))
    } else Left(PlatformServiceError("no organization found with given id"))
  }

  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */

  override def getUser(username: String): Option[User] = dao.findOne(MongoDBObject("userName" -> username))

  override def getUser(userId: ObjectId): Option[User] = dao.findOneById(userId)

  override def getUser(username: String, provider: String): Option[User] = {
    logger.debug(s"[getUser]: $username, $provider")
    val query = MongoDBObject("userName" -> username, "provider" -> provider)
    logger.trace(s"${dao.count(query)}")
    dao.findOne(query)
  }

  override def getPermissions(username: String, orgId: ObjectId): Either[PlatformServiceError, Permission] = {
    dao.findOne(MongoDBObject("userName" -> username, "org.orgId" -> orgId)) match {
      case Some(u) => getPermissions(u, orgId)
      case None => Left(PlatformServiceError(s"could not find user $username with access to given organization: $orgId"))
    }
  }

  private def getPermissions(user: User, orgId: ObjectId): Either[PlatformServiceError, Permission] = {
    Permission.fromLong(user.org.pval) match {
      case Some(p) => Right(p)
      case None => Left(PlatformServiceError("uknown permission retrieved"))
    }
  }

  override def getUsers(orgId: ObjectId): Either[PlatformServiceError, Seq[User]] = {
    val c: SalatMongoCursor[User] = dao.find(MongoDBObject("org.orgId" -> orgId))
    val returnValue = Right(c.toSeq)
    c.close()
    returnValue
  }

  override def touchLastLogin(userId: String) = touch(userId, "lastLoginDate")

  private def touch(userId: String, field: String): Unit =
    getUser(userId) match {
      case Some(user) => {
        dao.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
          MongoDBObject(
            field -> new DateTime())),
          false, false, dao.collection.writeConcern)
        Right(user)
      }
      case None => Left(PlatformServiceError("no user found to update " + field))
    }

  override def touchRegistration(userId: String) = touch(userId, "registrationDate")

  override def updateUser(user: User): Either[PlatformServiceError, User] = {
    try {
      dao.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
        MongoDBObject(
          "userName" -> user.userName,
          "fullName" -> user.fullName,
          "email" -> user.email,
          "password" -> user.password)),
        false, false, dao.collection.writeConcern)
      getUser(user.id) match {
        case Some(u) => Right(u)
        case None => Left(PlatformServiceError("no user found that was just modified"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(PlatformServiceError("failed to update user", e))
    }
  }

  override def getOrg(user: User, p: Permission): Option[Organization] = {
    val org: Option[ObjectId] = if ((user.org.pval & p.value) == p.value) Some(user.org.orgId) else None
    org.flatMap(orgService.findOneById)
  }

  override def setOrganization(userId: ObjectId, orgId: ObjectId, p: Permission): Either[PlatformServiceError, Unit] = {
    import com.novus.salat.grater

    val userOrg = UserOrg(orgId, p.value)
    try {
      dao.update(MongoDBObject("_id" -> userId),
        MongoDBObject("$set" -> MongoDBObject("org" -> grater[UserOrg].asDBObject(userOrg))),
        false, false, dao.collection.writeConcern)
      Right(())
    } catch {
      case e: SalatDAOUpdateError => Left(PlatformServiceError("could add organization to user"))
    }
  }

  override def removeUser(username: String): Either[PlatformServiceError, Unit] = {
    getUser(username) match {
      case Some(user) => removeUser(user.id)
      case None => Left(PlatformServiceError("user could not be removed because it doesn't exist"))
    }
  }

  override def removeUser(userId: ObjectId): Either[PlatformServiceError, Unit] = {
    try {
      dao.removeById(userId)
      Right(())
    } catch {
      case e: SalatRemoveError => Left(PlatformServiceError("error occurred while removing user", e))
    }
  }

  override def getUserByEmail(email: String): Option[User] = dao.findOne(MongoDBObject("email" -> email))
}
