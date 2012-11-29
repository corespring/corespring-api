package web

import _root_.controllers.auth.Permission
import _root_.controllers.Log
import _root_.models.{User, RegistrationToken}
import securesocial.core._
import play.api.Application
import org.bson.types.ObjectId
import providers.Token
import securesocial.core.UserId
import securesocial.core.SocialUser
import scala.Some
import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime

/**
 * An implementation of the UserService
 */
class CoreSpringUserService(application: Application) extends UserServicePlugin(application) {


  def find(id: UserId): Option[SocialUser] = {
    // id.id has the username
    Log.i("looking for %s(%s)".format(id.id, id.providerId))

    User.getUser(id.id, id.providerId) map {
      u =>
        Log.i("found user = " + u.userName)
        SocialUser(
          id,
          "",
          "",
          u.fullName,
          Some(u.email),
          None,
          AuthenticationMethod.UserPassword,
          passwordInfo = Some(PasswordInfo(u.password)
          )
        )
    }
  }

  def save(user: SocialUser) {
    if (User.getUser(user.id.id).isEmpty) {
      val corespringUser =
        User(
          user.id.id,
          user.fullName,
          user.email.getOrElse(""),
          Seq(),
          user.passwordInfo.getOrElse(PasswordInfo("")).password,
          user.id.providerId,
          new ObjectId())

      // hardcode this org id for now?
      val corespringId = new ObjectId("502404dd0364dc35bb39339a")
      User.insertUser(corespringUser, corespringId, Permission.All, checkOrgId = false)
    }
  }

  def findByEmailAndProvider(email: String, providerId: String) = None

  def findByEmail(email: String, providerId: String) = None

  def save(token: Token) {
    val newToken = RegistrationToken(token.uuid, token.email, Some(token.creationTime), Some(token.expirationTime))
    RegistrationToken.insert(newToken)
  }

  def findToken(token: String) = {
    val cursor = RegistrationToken.find(MongoDBObject(RegistrationToken.Uuid -> token))
    if (cursor.isEmpty)
      None
    else {
      val regToken = cursor.toList.head
      Some(Token(regToken.uuid, regToken.email, regToken.creationTime.get, regToken.expirationTime.get, true))
    }
  }

  def deleteToken(uuid: String) {
    RegistrationToken.findOne(MongoDBObject(RegistrationToken.Uuid -> uuid)) match {
      case Some(regToken) => RegistrationToken.remove(regToken)
      case _ => println("No such token found")
    }
  }

  def deleteExpiredTokens() {
    val currentTime = new DateTime()
    RegistrationToken.remove(MongoDBObject(RegistrationToken.Expires -> MongoDBObject("$lt" -> currentTime)))
  }
}
