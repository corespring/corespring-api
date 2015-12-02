package org.corespring.services.salat

import com.mongodb.{ WriteConcern, DBObject }
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatDAOUpdateError, SalatMongoCursor, SalatRemoveError }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, User, UserOrg }
import org.corespring.{ services => interface }
import org.joda.time.DateTime

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

class UserService(
  val dao: SalatDAO[User, ObjectId],
  val context: Context,
  orgService: interface.OrganizationService) extends interface.UserService with HasDao[User, ObjectId] {

  def logger: Logger = Logger(classOf[UserService])

  private implicit val ctx = context

  //TODO: Ensure Unique: userName - first need to remove duplicates - this script will find them:
  /**
   * db.users.aggregate([
   * { "$group": {
   * "_id": "$userName",
   * "count": { "$sum": 1 }
   * }},
   * { "$match": {
   * "count": { "$gt": 1 }
   * }}
   * ])
   */

  /**
   * insert a user into the database as a member of the given organization,
   * along with their private organization and collection
   * @param user
   * @return the user that was inserted
   */
  override def insertUser(user: User): Validation[PlatformServiceError, User] = for {
    _ <- orgService.findOneById(user.org.orgId).toSuccess(PlatformServiceError(s"Can't find org with id: ${user.org.orgId}"))
    _ <- if (getUser(user.userName).isEmpty) Success() else Failure(PlatformServiceError(s"The username: ${user.userName} already exists"))
    id <- dao.insert(user, dao.collection.writeConcern).toSuccess(PlatformServiceError(s"Insert failed for user ${user.userName}"))
  } yield user.copy(id = id)

  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */

  override def getUser(username: String): Option[User] = getUser(MongoDBObject("userName" -> username))

  override def getUser(userId: ObjectId): Option[User] = getUser(MongoDBObject("_id" -> userId))

  override def getUser(username: String, provider: String): Option[User] = getUser(MongoDBObject("userName" -> username, "provider" -> provider))

  override def getUserByEmail(email: String): Option[User] = getUser(MongoDBObject("email" -> email))

  private def getUser(query: DBObject): Option[User] = {
    logger.debug(s"[getUser]: query $query")
    val result = dao.findOne(query)
    logger.debug(s"[getUser]: result $result")
    result
  }

  override def getPermissions(username: String, orgId: ObjectId): Validation[PlatformServiceError, Option[Permission]] = for {
    u <- dao.findOne(MongoDBObject("userName" -> username, "org.orgId" -> orgId)).toSuccess(PlatformServiceError(s"can't find user with username: $username"))
  } yield Permission.fromLong(u.org.pval)

  override def getUsers(orgId: ObjectId): Stream[User] = dao.find(MongoDBObject("org.orgId" -> orgId)).toStream

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
    import scalaz.Scalaz._

    lazy val ensureUserNameIsntTaken = {

      val nameTaken = Failure(PlatformServiceError(s"The userName ${user.userName} is taken"))

      //Note: until we fix the unique constraint in the db for userName we need to check if we find multiple users.
      dao.find(MongoDBObject("userName" -> user.userName)).toSeq match {
        case Nil => Success()
        case Seq(u) if u.id == user.id => Success()
        case Seq(u) if u.id != user.id => nameTaken
        case head :: xs => {
          logger.warn(s"Found multiple users with the same name - this can happen at the moment because we don't ensureUnique on the userName: ${head +: xs}")
          //For now we're going to use the head
          if (head.id == user.id) Success() else nameTaken
        }
      }
    }

    lazy val applyUpdate = try {
      val result = dao.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
        MongoDBObject(
          "userName" -> user.userName,
          "fullName" -> user.fullName,
          "email" -> user.email,
          "password" -> user.password)),
        false, false, dao.collection.writeConcern)

      if (result.getN == 1) Success() else Failure(PlatformServiceError("Update failed"))

    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError(s"Update failed: $e"))
      case t: Throwable => Failure(PlatformServiceError(s"Update failed: ${t.getMessage}"))
    }

    for {
      dbUser <- getUser(user.id).toSuccess(PlatformServiceError("Can't update a user that isn't in saved, call 'insertUser' first."))
      _ <- ensureUserNameIsntTaken
      _ <- applyUpdate
    } yield user
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
