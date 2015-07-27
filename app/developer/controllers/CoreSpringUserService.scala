package developer.controllers

import com.mongodb.casbah.commons.MongoDBObject
import developer.ServiceLookup
import developer.models.RegistrationToken
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.common.log.PackageLogging
import org.corespring.models.auth.Permission
import org.corespring.models.{UserOrg, User}
import org.joda.time.DateTime
import play.api.Application
import securesocial.core._
import securesocial.core.providers.Token
import securesocial.core.providers.utils.PasswordHasher

/**
 * An implementation of the UserService
 */
class CoreSpringUserService(application: Application) extends UserServicePlugin(application) with PackageLogging {

  override def find(id: IdentityId): Option[SocialUser] = {
    // id.id has the username
    logger.debug("looking for %s(%s)".format(id.userId, id.providerId))

    ServiceLookup.userService.getUser(id.userId, id.providerId) map {
      u =>

        logger.debug(s"Found user: $u")

        SocialUser(
          id,
          "",
          "",
          u.fullName,
          Some(u.email),
          None,
          AuthenticationMethod.UserPassword,
          passwordInfo = Some(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = u.password)))
    }
  }

  override def save(user: Identity): Identity = {
    ServiceLookup.userService.getUser(user.identityId.userId, user.identityId.providerId) match {
      case None => {
        val corespringUser =
          User(
            user.identityId.userId,
            user.fullName,
            user.email.getOrElse(""),
            None,
            None,
            UserOrg(AppConfig.demoOrgId, Permission.Read.value),
            user.passwordInfo.getOrElse(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = "")).password,
            user.identityId.providerId,
            new ObjectId(),
            false)

        ServiceLookup.userService.insertUser(corespringUser, AppConfig.demoOrgId, Permission.Read, checkOrgId = false)
        user
      }
      case Some(existingUser) => {
        val password = user.passwordInfo.getOrElse(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = "")).password
        val withPassword = existingUser.copy(password = password)
        ServiceLookup.userService.updateUser(withPassword)
        user
      }
    }
  }

  override def save(token: Token): Unit = {
    val newToken = RegistrationToken(token.uuid, token.email, Some(token.creationTime), Some(token.expirationTime), token.isSignUp)
    RegistrationToken.insert(newToken)
  }

  override def findByEmailAndProvider(email: String, providerId: String) = ServiceLookup.userService.getUserByEmail(email).map(CoreSpringUserService.toIdentity)

  override def findToken(token: String) = {
    RegistrationToken.findOne(MongoDBObject(RegistrationToken.Uuid -> token)).map(regToken =>
      Token(regToken.uuid, regToken.email, regToken.creationTime.get, regToken.expirationTime.get, regToken.isSignUp))
  }

  override def deleteToken(uuid: String) {
    RegistrationToken.findOne(MongoDBObject(RegistrationToken.Uuid -> uuid)) match {
      case Some(regToken) => RegistrationToken.remove(regToken)
      case _ => logger.info("No such token found")
    }
  }

  def deleteExpiredTokens(): Unit = {
    val currentTime = new DateTime()
    RegistrationToken.remove(MongoDBObject(RegistrationToken.Expires -> MongoDBObject("$lt" -> currentTime)))
  }
}

object CoreSpringUserService {

  def toIdentity(u: User): Identity = {
    SocialUser(
      IdentityId(u.userName, u.provider),
      "",
      "",
      u.fullName,
      Some(u.email),
      None,
      AuthenticationMethod.UserPassword,
      passwordInfo = Some(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = u.password)))
  }
}
