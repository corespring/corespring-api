package models

import org.bson.types.ObjectId
import play.api.Play.current
import com.novus.salat.dao._
import se.radley.plugin.salat._
import mongoContext._
import play.api.libs.json._
import play.api.libs.json.JsObject
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import controllers.auth.Permission
import controllers.{LogType, Log}


/**
 * A User
 */
case class User(var userName: String,
                 var fullName: String,
                 var email: String,
                 var orgs: Seq[UserOrg] = Seq(),
                 var privateColl: ObjectId = new ObjectId(),
                 var id: ObjectId = new ObjectId()
               )

object User extends ModelCompanion[User, ObjectId] {
  val userName = "userName"
  val fullName = "fullName"
  val email = "email"
  val orgs = "orgs"
  val privateColl = "privateColl"

  val collection = mongoCollection("users")
  val dao = new SalatDAO[User, ObjectId]( collection = collection ) {}

  /**
   * insert a user into the database as a member of the given organization, along with their private organization and collection
   * @param user
   * @param orgId - the organization that the given user belongs to
   * @return the user that was inserted
   */
  def insertUser(user: User, orgId: ObjectId, p: Permission, checkOrgId:Boolean = true, checkUsername:Boolean = true): Either[ApiError, User] = {
    if (!checkOrgId || Organization.findOneById(orgId).isDefined){
      if (!checkUsername || getUser(user.userName).isEmpty){
            val privateColl = new ContentCollection(user.fullName,true)
            ContentCollection.insert(privateColl) match {
              case Some(_) => {
                user.id = new ObjectId
                user.privateColl = privateColl.id
                user.orgs =  List() :+ UserOrg(orgId,p.value)
                User.insert(user) match {
                  case Some(id) => {
                    Right(user)
                  }
                  case None => Left(ApiError(ApiError.DatabaseError,"error inserting user"))
                }
              }
              case None => Left(ApiError(ApiError.DatabaseError,"failed to insert content collection"))
            }

      }else Left(ApiError(ApiError.IllegalState,"user already exists"))
    }else Left(ApiError(ApiError.NotFound,"no organization found with given id"))
  }
  def removeUser(username: String): Either[ApiError,Unit] = {
    getUser(username) match {
      case Some(user) => removeUser(user.id)
      case None => Left(ApiError(ApiError.NotFound,"user could not be removed because it doesn't exist"))
    }
  }
  def removeUser(userId: ObjectId): Either[ApiError,Unit] = {
    try{
      User.removeById(userId)
      Right(())
    }catch{
      case e:SalatRemoveError => Left(ApiError(ApiError.DatabaseError,e.getMessage))
    }
  }
  def updateUser(user:User): Either[ApiError,User] = {
    try{
      User.update(MongoDBObject("_id" -> user.id), MongoDBObject("$set" ->
        MongoDBObject(User.userName -> user.userName, User.fullName -> user.fullName, User.email -> user.email)),
        false,false,User.collection.writeConcern)
      User.findOneById(user.id) match {
        case Some(u) => Right(u)
        case None => Left(ApiError(ApiError.IllegalState,"no user found that was just modified",LogType.printFatal))
      }
    }catch{
      case e:SalatDAOUpdateError => Left(ApiError(ApiError.DatabaseError,e.getMessage))
    }
  }
  /**
   * return the user from the database based on the given username, or None if the user wasn't found
   * @param username
   * @return
   */
  def getUser(username: String): Option[User] = User.findOne(MongoDBObject(User.userName -> username))

  //
  implicit object UserWrites extends Writes[User] {
    def writes(user: User) = {
      JsObject(
        List(
          "id" -> JsString(user.id.toString),
          "userName" -> JsString(user.userName),
          "fullName" -> JsString(user.fullName),
          "email" -> JsString(user.email)
        )
      )
    }
  }
}
case class UserOrg(var orgId: ObjectId, var pval: Long)
object UserOrg{
  val orgId: String = "orgId"
  val pval: String = "pval"
}



