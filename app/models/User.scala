package models

import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.dao._
import controllers.InternalError
import controllers.auth.Permission
import dao.SalatDAOUpdateError
import dao.SalatMongoCursor
import dao.SalatRemoveError
import mongoContext._
import play.api.Play
import play.api.Play.current
import play.api.libs.json._
import scala.Left
import scala.Right
import scala.Some
import se.radley.plugin.salat._
import search.Searchable
import securesocial.core.UserId


case class User(var userName: String = "",
                var fullName: String = "",
                var email: String = "",
                var org: Option[UserOrg] = None,
                var password: String = "",
                var provider: String = "userpass",
                var id: ObjectId = new ObjectId()
                 ) {
  def hasRegisteredOrg: Boolean = org.isDefined
}

object User extends ModelCompanion[User, ObjectId] with Searchable {
  val userName = "userName"
  val fullName = "fullName"
  val email = "email"
  val org = "org"
  val password = "password"
  val provider = "provider"
  val hasRegisteredOrg = "hasRegisteredOrg";

  val collection = mongoCollection("users")
  val dao = new SalatDAO[User, ObjectId](collection = collection) {}

  /**
   * insert a user into the database as a member of the given organization, along with their private organization and collection
   * @param user
   * @param orgId - the organization that the given user belongs to
   * @return the user that was inserted
   */
  def insertUser(user: User, orgId: ObjectId, p: Permission, checkOrgId: Boolean = true, checkUsername: Boolean = true): Either[InternalError, User] = {
    if (!checkOrgId || Organization.findOneById(orgId).isDefined) {
      if (!checkUsername || getUser(user.userName).isEmpty) {
        if (Play.isProd) user.id = new ObjectId
        user.org = Some(UserOrg(orgId, p.value))
        User.insert(user) match {
          case Some(id) => {
            Right(user)
          }
          case None => Left(InternalError("error inserting user"))
        }
      } else Left(InternalError("user already exists"))
    } else Left(InternalError("no organization found with given id"))
  }

  def removeUser(username: String): Either[InternalError, Unit] = {
    getUser(username) match {
      case Some(user) => removeUser(user.id)
      case None => Left(InternalError("user could not be removed because it doesn't exist"))
    }
  }

  def removeUser(userId: ObjectId): Either[InternalError, Unit] = {
    try {
      User.removeById(userId)
      Right(())
    } catch {
      case e: SalatRemoveError => Left(InternalError("error occured while removing user", e))
    }
  }

  def updateUser(user: User): Either[InternalError, User] = {
    try {
      User.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
        MongoDBObject(User.userName -> user.userName, User.fullName -> user.fullName, User.email -> user.email, User.password -> user.password)),
        false, false, User.collection.writeConcern)
      User.findOneById(user.id) match {
        case Some(u) => Right(u)
        case None => Left(InternalError("no user found that was just modified"))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("failed to update user", e))
    }
  }

  def addOrganization(userId: ObjectId, orgId: ObjectId, p: Permission): Either[InternalError, Unit] = {
    val userOrg = UserOrg(orgId, p.value)
    try {
      User.update(MongoDBObject("_id" -> userId),
        MongoDBObject("$set" -> MongoDBObject("org" -> grater[UserOrg].asDBObject(userOrg))),
        false, false, defaultWriteConcern);
      Right(())
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("could add organization to user"))
    }
  }

  def getOrg(user: User, p: Permission): Option[Organization] = {
    val orgs: Option[ObjectId] = user.org.filter(uo => (uo.pval & p.value) == p.value).map(uo => uo.orgId)
    orgs.flatMap(Organization.findOneById)
  }

  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */
  def getUser(username: String): Option[User] = User.findOne(MongoDBObject(User.userName -> username))

  def getUser(userId: UserId): Option[User] =
    User.findOne(
      MongoDBObject(User.userName -> userId.id, User.provider -> userId.providerId)
    )

  def getUser(username: String, provider: String): Option[User] =
    User.findOne(
      MongoDBObject(User.userName -> username, User.provider -> provider)
    )

  def getUsers(orgId: ObjectId): Either[InternalError, Seq[User]] = {
    val c: SalatMongoCursor[User] = User.find(MongoDBObject(User.org + "." + UserOrg.orgId -> orgId))
    val returnValue = Right(c.toSeq)
    c.close();
    returnValue
  }

  def removeOrganization(userId: ObjectId, orgId: ObjectId): Either[InternalError, Unit] = {
    User.findOneById(userId) match {
      case Some(user) => try {
        user.org = user.org.filter(_.orgId != orgId)
        User.update(MongoDBObject("_id" -> userId), user, false, false, User.defaultWriteConcern)
        Right(())
      } catch {
        case e: SalatDAOUpdateError => Left(InternalError(e.getMessage))
      }
      case None => Left(InternalError("could not find user"))
    }
  }

  def getPermissions(username: String, orgId: ObjectId): Either[InternalError, Permission] = {
    User.findOne(MongoDBObject(User.userName -> username, "org.orgId" -> orgId)) match {
      case Some(u) => getPermissions(u, orgId)
      case None => Left(InternalError("could not find user with access to given organization"))
    }
  }

  private def getPermissions(user: User, orgId: ObjectId): Either[InternalError, Permission] = {
    user.org.find(_.orgId == orgId) match {
      case Some(uo) => Permission.fromLong(uo.pval) match {
        case Some(p) => Right(p)
        case None => Left(InternalError("uknown permission retrieved"))
      }
      case None => Left(InternalError("userorg not found even though it was part of search requirement. this should never happen"))
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
    email
  )
}

case class UserOrg(var orgId: ObjectId, var pval: Long)

object UserOrg {
  val orgId: String = "orgId"
  val pval: String = "pval"
}



