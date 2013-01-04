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
import controllers._
import play.api.Play
import collection.mutable
import securesocial.core.UserId
import com.novus.salat._
import controllers.InternalError
import dao.SalatDAOUpdateError
import dao.SalatMongoCursor
import dao.SalatRemoveError
import scala.Left
import play.api.libs.json.JsString
import scala.Some
import scala.Right
import securesocial.core.UserId
import play.api.libs.json.JsObject


/**
 * A User
 */
case class User(var userName: String = "",
                 var fullName: String = "",
                 var email: String = "",
                 var orgs: Seq[UserOrg] = Seq(),
                 var password: String = "",
                 var provider : String = "userpass",
                 var hasRegisteredOrg:Boolean = false,
                 var id: ObjectId = new ObjectId()
               ) extends Identifiable

object User extends DBQueryable[User]{
  val userName = "userName"
  val fullName = "fullName"
  val email = "email"
  val orgs = "orgs"
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

  def addOrganization(userId: ObjectId, orgId: ObjectId, p : Permission):Either[InternalError,Unit] = {
    val userOrg = UserOrg(orgId,p.value)
    try{
      User.update(MongoDBObject("_id" -> userId),
        MongoDBObject("$set" -> MongoDBObject(hasRegisteredOrg -> true), "$addToSet" -> MongoDBObject("orgs" -> grater[UserOrg].asDBObject(userOrg))),
        false,false,defaultWriteConcern);
      Right(())
    }catch{
      case e:SalatDAOUpdateError => Left(InternalError("could add organization to user", addMessageToClientOutput = true))
    }
  }

  def getOrganizations(user: User, p: Permission):Seq[Organization] = {
    val orgs:Seq[ObjectId] = user.orgs.filter(uo => (uo.pval&p.value) == p.value).map(uo => uo.orgId)
    Utils.toSeq(Organization.find(MongoDBObject("_id" -> MongoDBObject("$in" -> orgs))))
  }
  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */
  def getUser(username: String): Option[User] = User.findOne(MongoDBObject(User.userName -> username))
  def getUser(userId: UserId) : Option[User] =
    User.findOne(
      MongoDBObject(User.userName -> userId.id, User.provider -> userId.providerId)
    )
  def getUser(username:String, provider:String) : Option[User] =
    User.findOne(
      MongoDBObject(User.userName -> username, User.provider -> provider)
    )
  def getUsers(orgId: ObjectId): Either[InternalError, Seq[User]] = {
    val c: SalatMongoCursor[User] = User.find(MongoDBObject(User.orgs + "." + UserOrg.orgId -> orgId))
    val returnValue = Right(c.toSeq)
    c.close(); returnValue
  }
  def removeOrganization(userId:ObjectId, orgId:ObjectId):Either[InternalError,Unit] = {
    //todo: add two-phase commit
    try{
      User.update(MongoDBObject("_id" -> userId, User.orgs+"."+UserOrg.orgId -> orgId), MongoDBObject("$set" ->MongoDBObject(User.orgs+".$" -> null)),
        false,false,User.defaultWriteConcern)
      User.update(MongoDBObject("_id"->userId),MongoDBObject("$pull" -> MongoDBObject(User.orgs -> null)),false,false,User.defaultWriteConcern)
      Right(())
    }catch {
      case e:SalatDAOUpdateError => Left(InternalError(e.getMessage))
    }
  }
  def getPermissions(userId:ObjectId, orgId:ObjectId):Either[InternalError,Permission] = {
    User.findOne(MongoDBObject("_id" -> userId, User.orgs+"."+UserOrg.orgId -> orgId)) match {
      case Some(u) => getPermissions(u,orgId)
      case None => Left(InternalError("could not find user with access to given organization",addMessageToClientOutput = true))
    }
  }
  def getPermissions(username:String, orgId:ObjectId):Either[InternalError,Permission] = {
    User.findOne(MongoDBObject(User.userName -> username, User.orgs+"."+UserOrg.orgId -> orgId)) match {
      case Some(u) => getPermissions(u,orgId)
      case None => Left(InternalError("could not find user with access to given organization",addMessageToClientOutput = true))
    }
  }
  private def getPermissions(user:User, orgId:ObjectId):Either[InternalError,Permission] = {
    user.orgs.find(_.orgId == orgId) match {
      case Some(uo) => Permission.fromLong(uo.pval) match {
        case Some(p) => Right(p)
        case None => Left(InternalError("uknown permission retrieved",addMessageToClientOutput = true))
      }
      case None => Left(InternalError("userorg not found even though it was part of search requirement. this should never happen"))
    }
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



