package models

import org.bson.types.ObjectId
import play.api.Play.current
import com.novus.salat.dao._
import se.radley.plugin.salat._
import mongoContext._
import play.api.libs.json._
import play.api.libs.json.JsObject
import com.mongodb.casbah.Imports._
import api.ApiError
import controllers.auth.Permission
import controllers.{QueryParser, InternalError, LogType, Log}
import play.api.Play
import collection.mutable


/**
 * A User
 */
case class User(var userName: String = "",
                 var fullName: String = "",
                 var email: String = "",
                 var orgs: Seq[UserOrg] = Seq(),
                 var password: String = "",
                 var id: ObjectId = new ObjectId()
               ) extends Identifiable

object User extends DBQueryable[User]{
  val userName = "userName"
  val fullName = "fullName"
  val email = "email"
  val orgs = "orgs"
  val password = "password"

  val collection = mongoCollection("users")
  val dao = new SalatDAO[User, ObjectId](collection = collection) {}

  /**
   * insert a user into the database as a member of the given organization, along with their private organization and collection
   * @param user
   * @param orgId - the organization that the given user belongs to
   * @return the user that was inserted
   */
  def insertUser(user: User, orgId: ObjectId, p: Permission, checkOrgId:Boolean = true, checkUsername:Boolean = true): Either[InternalError, User] = {
    if (!checkOrgId || Organization.findOneById(orgId).isDefined){
      if (!checkUsername || getUser(user.userName).isEmpty){
        if(Play.isProd) user.id = new ObjectId
        user.orgs =  List() :+ UserOrg(orgId,p.value)
        User.insert(user) match {
          case Some(id) => {
            Right(user)
          }
          case None => Left(InternalError("error inserting user",LogType.printFatal,true))
        }
      }else Left(InternalError("user already exists",LogType.printError,true))
    }else Left(InternalError("no organization found with given id",LogType.printError,true))
  }

  def removeUser(username: String): Either[InternalError, Unit] = {
    getUser(username) match {
      case Some(user) => removeUser(user.id)
      case None => Left(InternalError("user could not be removed because it doesn't exist",LogType.printError,true))
    }
  }

  def removeUser(userId: ObjectId): Either[InternalError, Unit] = {
    try {
      User.removeById(userId)
      Right(())
    } catch {
      case e: SalatRemoveError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("error occured while removing user")))
    }
  }

  def updateUser(user: User): Either[InternalError, User] = {
    try {
      User.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
        MongoDBObject(User.userName -> user.userName, User.fullName -> user.fullName, User.email -> user.email, User.password -> user.password)),
        false, false, User.collection.writeConcern)
      User.findOneById(user.id) match {
        case Some(u) => Right(u)
        case None => Left(InternalError("no user found that was just modified", LogType.printFatal, true))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to update user")))
    }
  }

  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */
  def getUser(username: String): Option[User] = User.findOne(MongoDBObject(User.userName -> username))

  def getUsers(orgId: ObjectId): Either[InternalError, Seq[User]] = {
    val c: SalatMongoCursor[User] = User.find(MongoDBObject(User.orgs + "." + UserOrg.orgId -> orgId))
    val returnValue = Right(c.toSeq)
    c.close(); returnValue
  }
  //
  implicit object UserWrites extends Writes[User] {
    def writes(user: User) = {
      var list = List[(String,JsString)]()
      if ( user.email.nonEmpty ) list = ("email" -> JsString(user.email)) :: list
      if ( user.fullName.nonEmpty ) list = ("fullName" -> JsString(user.fullName)) :: list
      if ( user.userName.nonEmpty ) list = ("userName" -> JsString(user.userName)) :: list
      list =  "id" -> JsString(user.id.toString) :: list
      JsObject(list)
    }
  }

  val queryFields:Seq[QueryField[User]] = Seq(
    QueryFieldObject[User]("_id",_.id, QueryField.valuefuncid),
    QueryFieldString[User](userName,_.userName),
    QueryFieldString[User](fullName,_.fullName),
    QueryFieldString[User](email,_.email)
  )
}

case class UserOrg(var orgId: ObjectId, var pval: Long)

object UserOrg {
  val orgId: String = "orgId"
  val pval: String = "pval"
}



