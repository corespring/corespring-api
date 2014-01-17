package org.corespring.test

import org.corespring.platform.core.models.User
import org.joda.time.DateTime
import play.api.mvc.Cookie
import scala.Some
import securesocial.core._
import securesocial.core.providers.utils.PasswordHasher
import play.api.cache.Cache

trait SecureSocialHelpers {

  def secureSocialCookie(u: Option[User], expires: Option[DateTime] = None): Option[Cookie] = u.map { user =>

    val authenticator: Authenticator = Authenticator.create(toIdentity(user)) match {
      case Left(e) => throw new RuntimeException(e.getMessage)
      case Right(a) => a
    }
    authenticator.toCookie
  }


  def expiredSecureSocialCookie(u: Option[User]): Option[Cookie] = u.map { user =>

    val authenticator: Authenticator = Authenticator.create(toIdentity(user)) match {
      case Left(e) => throw new RuntimeException(e)
      case Right(a) => a
    }

    import DateTime.now
    import play.api.Play.current
    val creationDate = now.minusDays(3)
    val expirationDate = now.minusDays(1)
    val lastUsed = now.minusDays(2)
    val withExpires = authenticator.copy(creationDate = creationDate,
      expirationDate = expirationDate,
      lastUsed = lastUsed)

    //Note: SecureSocial needs to look up the authenticator from the cache
    Cache.set(withExpires.id, withExpires)
    withExpires.toCookie
  }

  //TODO: From CorespringUserService
  private def toIdentity(u: User): Identity = {
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
