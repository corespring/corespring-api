package org.corespring.platform.core.models

import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.dao._
import dao.SalatDAOUpdateError
import dao.SalatMongoCursor
import dao.SalatRemoveError
import org.corespring.common.config.AppConfig
import org.corespring.common.log.ClassLogging
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.CorespringInternalError
import org.corespring.platform.core.models.search.Searchable
import org.joda.time.DateTime
import play.api.Play
import play.api.Play.current
import play.api.libs.json._
import se.radley.plugin.salat._
import securesocial.core.IdentityId

case class User(var userName: String = "",
  var fullName: String = "",
  var email: String = "",
  var lastLoginDate: Option[DateTime] = None,
  var registrationDate: Option[DateTime] = None,
  var org: UserOrg = UserOrg(AppConfig.demoOrgId, Permission.Read.value),
  var password: String = "",
  var provider: String = "userpass",
  var id: ObjectId = new ObjectId()) {
  def hasRegisteredOrg: Boolean = org.orgId != AppConfig.demoOrgId
}

object User extends ModelCompanion[User, ObjectId] with Searchable with ClassLogging {
  val userName = "userName"
  val fullName = "fullName"
  val email = "email"
  val orgKey = "org"
  val password = "password"
  val provider = "provider"
  val lastLoginDate = "lastLoginDate"
  val registrationDate = "registrationDate"

  val collection = mongoCollection("users")

  import org.corespring.platform.core.models.mongoContext.context

  val dao = new SalatDAO[User, ObjectId](collection = collection) {}

  object Dbo {
    def orgIdIn(orgIds: ObjectId*): DBObject = MongoDBObject(User.orgKey + "." + UserOrg.orgId -> MongoDBObject("$in" -> orgIds))
  }

  /**
   * insert a user into the database as a member of the given organization, along with their private organization and collection
   * @param user
   * @param orgId - the organization that the given user belongs to
   * @return the user that was inserted
   */
  def insertUser(user: User, orgId: ObjectId, p: Permission, checkOrgId: Boolean = true, checkUsername: Boolean = true): Either[CorespringInternalError, User] = {
    if (!checkOrgId || Organization.findOneById(orgId).isDefined) {
      if (!checkUsername || getUser(user.userName).isEmpty) {
        if (Play.isProd) user.id = new ObjectId
        user.org = UserOrg(orgId, p.value)
        User.insert(user) match {
          case Some(id) => {
            Right(user)
          }
          case None => Left(CorespringInternalError("error inserting user"))
        }
      } else Left(CorespringInternalError("user already exists"))
    } else Left(CorespringInternalError("no organization found with given id"))
  }

  def removeUser(username: String): Either[CorespringInternalError, Unit] = {
    getUser(username) match {
      case Some(user) => removeUser(user.id)
      case None => Left(CorespringInternalError("user could not be removed because it doesn't exist"))
    }
  }

  def removeUser(userId: ObjectId): Either[CorespringInternalError, Unit] = {
    try {
      User.removeById(userId)
      Right(())
    } catch {
      case e: SalatRemoveError => Left(CorespringInternalError("error occured while removing user", e))
    }
  }

  def touchLastLogin(userId: IdentityId) = touch(userId, User.lastLoginDate)
  def touchRegistration(userId: IdentityId) = touch(userId, User.registrationDate)

  def touch(userId: IdentityId, field: String) = {
    User.getUser(userId) match {
      case Some(user) => {
        User.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
          MongoDBObject(
            field -> new DateTime())),
          false, false, User.collection.writeConcern)
        Right(user)
      }
      case None => Left(CorespringInternalError("no user found to update " + field))
    }
  }

  def updateUser(user: User): Either[CorespringInternalError, User] = {
    try {
      User.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
        MongoDBObject(
          User.userName -> user.userName,
          User.fullName -> user.fullName,
          User.email -> user.email,
          User.password -> user.password)),
        false, false, User.collection.writeConcern)
      User.findOneById(user.id) match {
        case Some(u) => Right(u)
        case None => Left(CorespringInternalError("no user found that was just modified"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(CorespringInternalError("failed to update user", e))
    }
  }

  def setOrganization(userId: ObjectId, orgId: ObjectId, p: Permission): Either[CorespringInternalError, Unit] = {
    val userOrg = UserOrg(orgId, p.value)
    try {
      User.update(MongoDBObject("_id" -> userId),
        MongoDBObject("$set" -> MongoDBObject("org" -> grater[UserOrg].asDBObject(userOrg))),
        false, false, defaultWriteConcern);
      Right(())
    } catch {
      case e: SalatDAOUpdateError => Left(CorespringInternalError("could add organization to user"))
    }
  }

  def getOrg(user: User, p: Permission): Option[Organization] = {
    val org: Option[ObjectId] = if ((user.org.pval & p.value) == p.value) Some(user.org.orgId) else None
    org.flatMap(Organization.findOneById)
  }

  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */
  def getUser(username: String): Option[User] = User.findOne(MongoDBObject(User.userName -> username))

  def getUser(userId: IdentityId): Option[User] = getUser(userId.userId, userId.providerId)

  def getUser(username: String, provider: String): Option[User] = {
    logger.debug(s"[getUser]: $username, $provider")
    val query = MongoDBObject(User.userName -> username, User.provider -> provider)
    logger.trace(s"${User.count(query)}")
    User.findOne(query)
  }

  def getUsers(orgId: ObjectId): Either[CorespringInternalError, Seq[User]] = {
    val c: SalatMongoCursor[User] = User.find(MongoDBObject(User.orgKey + "." + UserOrg.orgId -> orgId))
    val returnValue = Right(c.toSeq)
    c.close();
    returnValue
  }

  def getPermissions(username: String, orgId: ObjectId): Either[CorespringInternalError, Permission] = {
    User.findOne(MongoDBObject(User.userName -> username, "org.orgId" -> orgId)) match {
      case Some(u) => getPermissions(u, orgId)
      case None => Left(CorespringInternalError(s"could not find user $username with access to given organization: $orgId"))
    }
  }

  private def getPermissions(user: User, orgId: ObjectId): Either[CorespringInternalError, Permission] = {
    Permission.fromLong(user.org.pval) match {
      case Some(p) => Right(p)
      case None => Left(CorespringInternalError("uknown permission retrieved"))
    }
  }

  implicit object UserWrites extends Writes[User] {
    def writes(user: User) = {
      var list = List[(String, JsString)]()
      if (user.email.nonEmpty) list = ("email" -> JsString(user.email)) :: list
      if (user.fullName.nonEmpty) list = ("fullName" -> JsString(user.fullName)) :: list
      if (user.userName.nonEmpty) list = ("userName" -> JsString(user.userName)) :: list
      list = "id" -> JsString(user.id.toString) :: list
      JsObject(list)
    }
  }

  override val searchableFields = Seq(
    userName,
    fullName,
    email)
}

case class UserOrg(var orgId: ObjectId, var pval: Long)

object UserOrg {
  val orgId: String = "orgId"
  val pval: String = "pval"
}

