package developer.controllers

import com.mongodb.casbah.commons.MongoDBObject
import developer.models.RegistrationToken
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.Application
import scala.Some
import securesocial.core._
import securesocial.core.providers.utils.PasswordHasher
import securesocial.core.providers.Token
import org.corespring.platform.core.models.{UserOrg, User}
import org.corespring.common.config.AppConfig
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.auth.Permission

/**
 * An implementation of the UserService
 */
class CoreSpringUserService(application: Application) extends UserServicePlugin(application) with PackageLogging {

  override def find(id: UserId): Option[SocialUser] = {
    // id.id has the username
    Logger.debug("looking for %s(%s)".format(id.id, id.providerId))

    User.getUser(id) map {
      u =>

        Logger.debug(s"Found user: $u")

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

  override def save(user: Identity): Identity = {
    User.getUser(user.id.id, user.id.providerId) match {
      case None => {
        val corespringUser =
          User(
            user.id.id,
            user.fullName,
            user.email.getOrElse(""),
            None,
            None,
            UserOrg(AppConfig.demoOrgId,Permission.Read.value),
            user.passwordInfo.getOrElse(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = "")).password,
            user.id.providerId,
            new ObjectId())

        User.insertUser(corespringUser, AppConfig.demoOrgId, Permission.Read, checkOrgId = false)
        user
      }
      case Some(existingUser) => {
        existingUser.password = user.passwordInfo.getOrElse(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = "")).password
        User.save(existingUser)
        user
      }
    }
  }

  override def save(token: Token): Unit = {
    val newToken = RegistrationToken(token.uuid, token.email, Some(token.creationTime), Some(token.expirationTime), token.isSignUp)
    RegistrationToken.insert(newToken)
  }

  override def findByEmailAndProvider(email: String, providerId: String) = User.findOne(MongoDBObject(User.email -> email)).map(CoreSpringUserService.toIdentity)


  override def findToken(token: String) = {
    RegistrationToken.findOne(MongoDBObject(RegistrationToken.Uuid -> token)).map(regToken =>
      Token(regToken.uuid, regToken.email, regToken.creationTime.get, regToken.expirationTime.get, regToken.isSignUp)
    )
  }

  override def deleteToken(uuid: String) {
    RegistrationToken.findOne(MongoDBObject(RegistrationToken.Uuid -> uuid)) match {
      case Some(regToken) => RegistrationToken.remove(regToken)
      case _ => Logger.info("No such token found")
    }
  }

  def deleteExpiredTokens(): Unit = {
    val currentTime = new DateTime()
    RegistrationToken.remove(MongoDBObject(RegistrationToken.Expires -> MongoDBObject("$lt" -> currentTime)))
  }
}

object CoreSpringUserService{

  def toIdentity(u:User) : Identity = {
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
  }
}
