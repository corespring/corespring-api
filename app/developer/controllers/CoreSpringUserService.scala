package developer.controllers

import _root_.controllers.auth.Permission
import _root_.controllers.Log
import _root_.models.{User}
import securesocial.core._
import play.api.Application
import org.bson.types.ObjectId
import providers.Token
import providers.utils.PasswordHasher
import securesocial.core.UserId
import securesocial.core.SocialUser
import scala.Some
import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime
import se.radley.plugin.salat.SalatPlugin
import developer.models.RegistrationToken

/**
 * An implementation of the UserService
 */
class CoreSpringUserService(application: Application) extends UserServicePlugin(application) {

  def find(id: UserId): Option[SocialUser] = {
    // id.id has the username
    Log.i("looking for %s(%s)".format(id.id, id.providerId))

    User.getUser(id.id, id.providerId) map {
      u =>
        SocialUser(
          id,
          "",
          "",
          u.fullName,
          Some(u.email),
          None,
          AuthenticationMethod.UserPassword,
          passwordInfo = Some(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = u.password)
          )
        )
    }
  }

  def save(user: Identity) {
    User.getUser(user.id.id, user.id.providerId) match {
      case None =>
        val corespringUser =
          User(
            user.id.id,
            user.fullName,
            user.email.getOrElse(""),
            Seq(),
            user.passwordInfo.getOrElse(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = "")).password,
            user.id.providerId,
            false,
            new ObjectId())

        // hardcode this org id for now?
        val corespringId = new ObjectId("502404dd0364dc35bb39339a")

        User.insertUser(corespringUser, corespringId, Permission.Read, checkOrgId = false)

      case Some(existingUser) =>
        existingUser.password = user.passwordInfo.getOrElse(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = "")).password
        User.save(existingUser)
    }
  }

  def findByEmailAndProvider(email: String, providerId: String) = {
    User.findOne(MongoDBObject(User.email -> email)).map(u =>
      SocialUser(
        UserId(u.userName, u.provider),
        "",
        "",
        u.fullName,
        Some(u.email),
        None,
        AuthenticationMethod.UserPassword,
        passwordInfo = Some(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = u.password))
      )
    )
  }

  def save(token: Token) {
    val newToken = RegistrationToken(token.uuid, token.email, Some(token.creationTime), Some(token.expirationTime), token.isSignUp)
    RegistrationToken.insert(newToken)
  }

  def findToken(token: String) = {
    RegistrationToken.findOne(MongoDBObject(RegistrationToken.Uuid -> token)).map(regToken =>
      Token(regToken.uuid, regToken.email, regToken.creationTime.get, regToken.expirationTime.get, regToken.isSignUp)
    )
  }

  def deleteToken(uuid: String) {
    RegistrationToken.findOne(MongoDBObject(RegistrationToken.Uuid -> uuid)) match {
      case Some(regToken) => RegistrationToken.remove(regToken)
      case _ => Log.i("No such token found")
    }
  }

  def deleteExpiredTokens() {
    val currentTime = new DateTime()
    try{
      RegistrationToken.remove(MongoDBObject(RegistrationToken.Expires -> MongoDBObject("$lt" -> currentTime)))
    }catch{
      case e:IllegalStateException => //this occurs if the app closes before this is called. should be safe to ignore
    }
  }
}
