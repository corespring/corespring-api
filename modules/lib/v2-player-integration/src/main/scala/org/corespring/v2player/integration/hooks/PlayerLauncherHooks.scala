package org.corespring.v2player.integration.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.{ PlayerJs, PlayerLauncherHooks => ContainerPlayerLauncherHooks }
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.services.UserService
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2player.integration.cookies.V2PlayerCookieWriter
import play.api.mvc._

import scala.concurrent.Future
import scalaz._

trait PlayerLauncherHooks extends ContainerPlayerLauncherHooks with V2PlayerCookieWriter {
  def secureSocialService: SecureSocialService

  def userService: UserService

  def getOrgIdAndOptions(header: RequestHeader): Validation[String, (ObjectId, PlayerOptions)]

  /** A helper method to allow you to create a new session out of the existing and a variable number of Key values pairs */
  override def sumSession(s: Session, keyValues: (String, String)*): Session = {
    keyValues.foldRight(s)((kv: (String, String), acc: Session) => acc + (kv._1, kv._2))
  }

  override def playerJs(implicit header: RequestHeader): Future[PlayerJs] = load(header)

  override def editorJs(implicit header: RequestHeader): Future[PlayerJs] = load(header)

  private def load(implicit header: RequestHeader): Future[PlayerJs] = Future {
    getOrgIdAndOptions(header) match {
      case Success((orgId, opts)) => {
        val newSession = sumSession(header.session, playerCookies(orgId, Some(opts)): _*)
        PlayerJs(opts.secure, newSession)
      }
      case Failure(error) => PlayerJs(false, header.session, Seq(error))
    }

  }

}
