package developer.controllers

import org.corespring.legacy.ServiceLookup
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.registration.RegistrationToken
import org.corespring.models.{ UserOrg, User }
import play.api.{ Logger, Application }
import securesocial.core._
import securesocial.core.providers.Token
import securesocial.core.providers.utils.PasswordHasher

class CoreSpringUserService(application: Application) extends UserServicePlugin(application) {
  val logger = Logger(classOf[CoreSpringUserService])
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
            UserOrg(ServiceLookup.demoOrgId, Permission.Read.value),
            user.passwordInfo.getOrElse(PasswordInfo(hasher = PasswordHasher.BCryptHasher, password = "")).password,
            user.identityId.providerId,
            new ObjectId())

        ServiceLookup.userService.insertUser(corespringUser)
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
    ServiceLookup.registrationTokenService.createToken(newToken)
  }

  override def findByEmailAndProvider(email: String, providerId: String) = ServiceLookup.userService.getUserByEmail(email).map(CoreSpringUserService.toIdentity)

  override def findToken(uuid: String) = {
    ServiceLookup.registrationTokenService.findTokenByUuid(uuid).map(regToken =>
      Token(regToken.uuid, regToken.email, regToken.creationTime.get, regToken.expirationTime.get, regToken.isSignUp))
  }

  override def deleteToken(uuid: String) {
    ServiceLookup.registrationTokenService.deleteToken(uuid)
  }

  def deleteExpiredTokens(): Unit = {

    if (ServiceLookup.registrationTokenService != null) {
      ServiceLookup.registrationTokenService.deleteExpiredTokens()
    }
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
