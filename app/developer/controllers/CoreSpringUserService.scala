package developer.controllers

import _root_.controllers.auth.Permission
import _root_.models.User
import com.mongodb.casbah.commons.MongoDBObject
import common.config.AppConfig
import developer.models.RegistrationToken
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.Application
import scala.Some
import securesocial.core._
import securesocial.core.providers.utils.PasswordHasher
import securesocial.core.providers.Token
import common.log.PackageLogging

/**
 * An implementation of the UserService
 */
class CoreSpringUserService(application: Application) extends UserServicePlugin(application) with PackageLogging {

  def find(id: UserId): Option[SocialUser] = {
    // id.id has the username
    Logger.info("looking for %s(%s)".format(id.id, id.providerId))

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

  override def save(user : Identity) {
    User.getUser(user.id.id, user.id.providerId) match {
      case None =>
        val corespringUser =
          User(
            user.id.id,
            user.fullName,
            user.email.getOrElse(""),
            None,
            user.passwordInfo.getOrElse(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = "")).password,
            user.id.providerId,
            new ObjectId())

        User.insertUser(corespringUser, AppConfig.demoOrgId, Permission.Read, checkOrgId = false)

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
      case _ => Logger.info("No such token found")
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
