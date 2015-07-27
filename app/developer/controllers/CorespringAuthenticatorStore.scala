package developer.controllers

import securesocial.core.AuthenticatorStore
import play.api.Application
import securesocial.core.Authenticator
import play.api.cache.Cache
import play.api.Play.current

class CorespringAuthenticatorStore(app: Application) extends AuthenticatorStore(app) {

  def save(authenticator: Authenticator): Either[Error, Unit] = {
    Cache.set(authenticator.id, authenticator)
    Right(())
  }
  def find(id: String): Either[Error, Option[Authenticator]] = {
    try {
      Right(Cache.getAs[Authenticator](id))
    } catch {
      case e: IllegalArgumentException => Left(new Error(e))
    }
  }
  def delete(id: String): Either[Error, Unit] = {
    Cache.set(id, "", 1)
    Right(())
  }
}
